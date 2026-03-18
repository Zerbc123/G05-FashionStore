package vn.edu.fpt.fashionstore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.fashionstore.entity.*;
import vn.edu.fpt.fashionstore.repository.AccountRepository;
import vn.edu.fpt.fashionstore.repository.ReviewRepository;
import vn.edu.fpt.fashionstore.service.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/products")
public class ProductController {

    // --- THÊM MỚI 3 DÒNG NÀY ĐỂ XỬ LÝ ĐÁNH GIÁ ---
    @Autowired
    private ReviewService reviewService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private AccountRepository accountRepository;
    // ----------------------------------------------

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private ImageUploadService imageUploadService;

    // ========================================================================
    // 1. DANH SÁCH SẢN PHẨM (LIST)
    // URL: /products
    // ========================================================================
    @GetMapping
    public String showProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String priceRange,
            @RequestParam(defaultValue = "productName") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int pageSize,
            Model model) {

        // Xử lý priceRange để chuyển thành minPrice và maxPrice
        String currentPriceRange = "";
        if (priceRange != null && !priceRange.isBlank()) {
            String[] prices = priceRange.split("-");
            if (prices.length == 2) {
                try {
                    minPrice = Double.parseDouble(prices[0]);
                    maxPrice = Double.parseDouble(prices[1]);
                    currentPriceRange = priceRange;
                } catch (NumberFormatException e) {
                    // Ignore invalid format
                }
            }
        }

        // 1. Xử lý sắp xếp
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(sortDirection, sort));

        // 2. Gọi Service lấy dữ liệu
        Page<Product> productPage;

        // Debug: In ra các tham số nhận được từ UI
        System.out.println("=== UI PARAMETERS ===");
        System.out.println("keyword: " + keyword);
        System.out.println("categoryId: " + categoryId);
        System.out.println("categoryName: " + categoryName);
        System.out.println("size: " + size);
        System.out.println("color: " + color);
        System.out.println("minPrice: " + minPrice);
        System.out.println("maxPrice: " + maxPrice);
        System.out.println("priceRange: " + priceRange);
        System.out.println("hasFilter: " + hasFilter(keyword, categoryId, categoryName, size, color, minPrice, maxPrice));

        // Chỉ gọi searchAndFilterProducts khi thực sự có filter
        if (keyword != null && !keyword.isBlank() ||
                categoryId != null ||
                (categoryName != null && !categoryName.isBlank()) ||
                (size != null && !size.isBlank()) ||
                (color != null && !color.isBlank()) ||
                minPrice != null ||
                maxPrice != null) {
            System.out.println("Calling searchAndFilterProducts...");
            
            // Nếu có categoryName, ưu tiên lọc theo categoryName trước
            if (categoryName != null && !categoryName.isBlank()) {
                productPage = productService.filterByCategoryName(categoryName, pageable);
            } else {
                productPage = productService.searchAndFilterProducts(keyword, categoryId, size, color, minPrice, maxPrice, pageable);
            }
        } else {
            System.out.println("Calling getAllProducts...");
            productPage = productService.getAllProducts(pageable);
        }

        System.out.println("Result: " + productPage.getTotalElements() + " products found");
        System.out.println("=== END UI PARAMETERS ===");

        // 3. Xử lý trường hợp trang trống (khi đang ở trang 2 mà lọc ra ít kết quả)
        if (page > 0 && productPage.isEmpty() && productPage.getTotalElements() > 0) {
            pageable = PageRequest.of(0, pageSize, Sort.by(sortDirection, sort));
            if (keyword != null && !keyword.isBlank() ||
                    categoryId != null ||
                    (categoryName != null && !categoryName.isBlank()) ||
                    (size != null && !size.isBlank()) ||
                    (color != null && !color.isBlank()) ||
                    minPrice != null ||
                    maxPrice != null) {
                if (categoryName != null && !categoryName.isBlank()) {
                    productPage = productService.filterByCategoryName(categoryName, pageable);
                } else {
                    productPage = productService.searchAndFilterProducts(keyword, categoryId, size, color, minPrice, maxPrice, pageable);
                }
            } else {
                productPage = productService.getAllProducts(pageable);
            }
        }

        // 4. Đẩy dữ liệu ra View
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("allProducts", productService.getAllProductsWithVariants());
        model.addAttribute("currentPage", productPage.getNumber());
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalItems", productPage.getTotalElements());
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedCategoryName", categoryName);
        model.addAttribute("selectedSize", size);
        model.addAttribute("selectedColor", color);
        model.addAttribute("stockStatus", stockStatus);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("priceRange", currentPriceRange);

        List<Category> categories = productService.findAllCategories();
        model.addAttribute("categoryIds", categories); // Lưu ý: View đang dùng tên biến 'categoryIds'

        long total = productPage.getTotalElements();
        int start = total == 0 ? 0 : productPage.getNumber() * pageSize + 1;
        int end = Math.min(start + productPage.getNumberOfElements() - 1, (int) total);
        model.addAttribute("startItem", start);
        model.addAttribute("endItem", end);

        return "list";
    }

    // ========================================================================
    // 2. CHI TIẾT SẢN PHẨM
    // URL: /products/detail/{id}
    // ========================================================================
    @GetMapping("/detail/{id}")
    public String productDetails(
            @PathVariable("id") Long id,
            @RequestParam(name = "colorId", required = false) Integer colorId,
            @RequestParam(name = "sizeId", required = false) Integer sizeId,
            jakarta.servlet.http.HttpSession session,
            Model model) {

        Product product = productService.getProductById(id);
        if (product == null) {
            return "redirect:/products";
        }

        List<vn.edu.fpt.fashionstore.entity.ProductVariant> variants = product.getVariants();

        // Lấy danh sách Size/Màu duy nhất
        List<CategorySize> uniqueSizes = variants.stream()
                .map(vn.edu.fpt.fashionstore.entity.ProductVariant::getCategorySize)
                .filter(size -> size != null)
                .distinct()
                .collect(Collectors.toList());

        List<Color> uniqueColors = variants.stream()
                .map(vn.edu.fpt.fashionstore.entity.ProductVariant::getColor)
                .filter(color -> color != null)
                .distinct()
                .collect(Collectors.toList());

        // Xác định biến thể được chọn
        vn.edu.fpt.fashionstore.entity.ProductVariant selectedVariant = null;

        if (colorId != null && sizeId != null) {
            selectedVariant = variants.stream()
                    .filter(v -> v.getColor().getColorId() == colorId &&
                            v.getCategorySize().getCategorySizeId() == sizeId)
                    .findFirst()
                    .orElse(null);
        }

        // Mặc định biến thể đầu tiên
        if (selectedVariant == null && !variants.isEmpty()) {
            selectedVariant = variants.get(0);
        }

        model.addAttribute("product", product);
        model.addAttribute("variants", product.getVariants());
        model.addAttribute("uniqueSizes", uniqueSizes);
        model.addAttribute("uniqueColors", uniqueColors);
        model.addAttribute("selectedVariant", selectedVariant);

        // ==========================================
        // THÊM MỚI: XỬ LÝ DATA ĐÁNH GIÁ (REVIEW)
        // ==========================================

        // 1. Lấy danh sách đánh giá của sản phẩm này gửi ra HTML
        List<vn.edu.fpt.fashionstore.entity.Review> reviews = reviewService.getActiveReviewsByProduct(product);
        model.addAttribute("reviews", reviews);

        // 2. Tính trung bình sao (nếu có đánh giá)
        double averageRating = 0;
        if (!reviews.isEmpty()) {
            averageRating = reviews.stream().mapToInt(vn.edu.fpt.fashionstore.entity.Review::getRating).average().orElse(0.0);
        }
        model.addAttribute("averageRating", averageRating);

        // 3. Kiểm tra quyền được đánh giá (đã mua và nhận hàng chưa)
        // Kiểm tra quyền đánh giá mới
        Customer currentCustomer = getCurrentCustomer(session); // Hàm này bạn tự tùy chỉnh theo code hiện tại của file
        if (currentCustomer != null) {
            // Gọi 2 hàm đếm ra
            long purchaseCount = orderService.countSuccessfulPurchases(currentCustomer, id);
            long reviewCount = reviewRepository.countByCustomerAndProduct_ProductId(currentCustomer, id);

            // Nút "Viết đánh giá" chỉ hiện lên khi số lần mua thành công LỚN HƠN số lần đã review
            model.addAttribute("canReview", purchaseCount > reviewCount);
        } else {
            // Khách chưa đăng nhập thì mặc định ẩn nút
            model.addAttribute("canReview", false);
        }

        return "productdetails";
    }

    // ========================================================================
    // 5. PRODUCT IMAGE UPLOAD PAGE
    // URL: /products/{id}/upload-image
    // ========================================================================
    @GetMapping("/{id}/upload-image")
    public String uploadImagePage(@PathVariable("id") Long productId, Model model) {
        Product product = productService.getProductById(productId);
        if (product == null) {
            return "redirect:/products";
        }
        
        model.addAttribute("productId", productId);
        model.addAttribute("product", product);
        return "admin/product-image-upload";
    }

    // ========================================================================
    // 6. UPDATE PRODUCT VARIANT IMAGE
    // URL: /products/{id}/update-image
    // ========================================================================
    @PostMapping("/{id}/update-image")
    public String updateProductImage(
            @PathVariable("id") Long productId,
            @RequestParam("imageUrl") String imageUrl,
            @RequestParam(value = "variantId", required = false) Integer variantId,
            Model model) {
        
        try {
            Product product = productService.getProductById(productId);
            if (product != null && product.getVariants() != null && !product.getVariants().isEmpty()) {
                // Update first variant or specific variant if provided
                ProductVariant targetVariant = null;
                if (variantId != null) {
                    targetVariant = product.getVariants().stream()
                        .filter(v -> v.getVariantId() == variantId)
                        .findFirst()
                        .orElse(null);
                } else {
                    targetVariant = product.getVariants().get(0); // Default to first variant
                }
                
                if (targetVariant != null) {
                    targetVariant.setImageUrl(imageUrl);
                    // Here you would typically save the variant to database
                    // productVariantService.saveProductVariant(targetVariant);
                    model.addAttribute("success", "Image updated successfully!");
                }
            }
        } catch (Exception e) {
            model.addAttribute("error", "Failed to update image: " + e.getMessage());
        }
        
        return "redirect:/products/detail/" + productId;
    }

    // ========================================================================
    // 4. DEBUG FILTER ENDPOINT
    // URL: /products/debug
    // ========================================================================
    @GetMapping("/debug")
    @ResponseBody
    public String debugFilter(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String size,
            @RequestParam(defaultValue = "0") int page) {
        
        Pageable pageable = PageRequest.of(page, 12);
        Page<Product> result = productService.debugFilter(categoryId, color, size, pageable);
        
        return "Debug Filter Results:<br>" +
               "Category ID: " + categoryId + "<br>" +
               "Color: " + color + "<br>" +
               "Size: " + size + "<br>" +
               "Total Products Found: " + result.getTotalElements() + "<br>" +
               "Products on Page: " + result.getContent().size() + "<br>" +
               "Total Pages: " + result.getTotalPages() + "<br><br>" +
               "<small>Check console for detailed step-by-step debugging</small>";
    }

    // ========================================================================
    // 3. HÀM PHỤ TRỢ (CHECK FILTER)
    // ========================================================================
    private boolean hasFilter(String keyword, Long categoryId, String categoryName, String size, String color, Double minPrice, Double maxPrice) {
        return (keyword != null && !keyword.isBlank()) || categoryId != null ||
                (categoryName != null && !categoryName.isBlank()) ||
                (size != null && !size.isBlank()) || (color != null && !color.isBlank()) || 
                minPrice != null || maxPrice != null;
    }

    // --- THÊM MỚI HÀM NÀY ĐỂ LẤY KHÁCH HÀNG ---
    private vn.edu.fpt.fashionstore.entity.Customer getCurrentCustomer(jakarta.servlet.http.HttpSession session) {
        String email = (String) session.getAttribute("user");
        if (email == null) return null;
        java.util.Optional<vn.edu.fpt.fashionstore.entity.Account> accountOpt = accountRepository.findByEmail(email);
        if (accountOpt.isEmpty() || accountOpt.get().getCustomers().isEmpty()) return null;
        return accountOpt.get().getCustomers().get(0);
    }
}
