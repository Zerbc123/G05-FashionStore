package vn.edu.fpt.fashionstore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.fashionstore.entity.*;
import vn.edu.fpt.fashionstore.repository.AccountRepository;
import vn.edu.fpt.fashionstore.repository.ReviewRepository;
import vn.edu.fpt.fashionstore.service.AccountService;
import vn.edu.fpt.fashionstore.service.OrderService;
import vn.edu.fpt.fashionstore.service.ProductService;
import vn.edu.fpt.fashionstore.service.ReviewService;

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

    // ========================================================================
    // 1. DANH SÁCH SẢN PHẨM (LIST)
    // URL: /products
    // ========================================================================
    @GetMapping
    public String showProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int pageSize,
            @RequestParam(defaultValue = "productName") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {

        // 1. Xử lý sắp xếp
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(sortDirection, sort));

        // 2. Gọi Service lấy dữ liệu
        Page<Product> productPage;
        if (hasFilter(keyword, categoryId, size, minPrice, maxPrice)) {
            productPage = productService.searchAndFilterProducts(keyword, categoryId, size, minPrice, maxPrice, pageable);
        } else {
            productPage = productService.getAllProducts(pageable);
        }

        // 3. Xử lý trường hợp trang trống (khi đang ở trang 2 mà lọc ra ít kết quả)
        if (page > 0 && productPage.isEmpty() && productPage.getTotalElements() > 0) {
            pageable = PageRequest.of(0, pageSize, Sort.by(sortDirection, sort));
            if (hasFilter(keyword, categoryId, size, minPrice, maxPrice)) {
                productPage = productService.searchAndFilterProducts(keyword, categoryId, size, minPrice, maxPrice, pageable);
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
        model.addAttribute("selectedSize", size);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);

        List<Category> categories = productService.getAllCategoryIds();
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
    // 3. HÀM PHỤ TRỢ (CHECK FILTER)
    // ========================================================================
    private boolean hasFilter(String keyword, Long categoryId, String size, Double minPrice, Double maxPrice) {
        return (keyword != null && !keyword.isBlank()) || categoryId != null ||
                (size != null && !size.isBlank()) || minPrice != null || maxPrice != null;
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