package vn.edu.fpt.fashionstore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.edu.fpt.fashionstore.entity.Category;
import vn.edu.fpt.fashionstore.entity.CategorySize;
import vn.edu.fpt.fashionstore.entity.Color;
import vn.edu.fpt.fashionstore.entity.Product;
import vn.edu.fpt.fashionstore.entity.ProductVariant;
import vn.edu.fpt.fashionstore.service.ProductService;
import vn.edu.fpt.fashionstore.service.ProductVariantService;
import vn.edu.fpt.fashionstore.service.CloudinaryService;
import vn.edu.fpt.fashionstore.repository.*;
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

    @Autowired
    private ProductVariantService productVariantService;
    
    @Autowired
    private CategoriesRepository categoryRepository;
    
    @Autowired
    private ColorRepository colorRepository;
    
    @Autowired
    private CategorySizeRepository categorySizeRepository;
    
    @Autowired
    private CloudinaryService cloudinaryService;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ProductVariantRepository productVariantRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;

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
            @RequestParam(defaultValue = "productId") String sort,
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
            // TODO: Fix countSuccessfulPurchases method
            long purchaseCount = 0; // orderService.countSuccessfulPurchases(currentCustomer, id);
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
    // 3. HELPER METHODS
    // ========================================================================
    private boolean hasFilter(String keyword, Long categoryId, String size,
            Double minPrice, Double maxPrice) {

        return (keyword != null && !keyword.isBlank())
                || categoryId != null
                || (size != null && !size.isBlank())
                || minPrice != null
                || maxPrice != null;
    }

    // ========================================================================
    // 4. ADMIN - LIST PRODUCT
    // ========================================================================
    @GetMapping("/admin/products")
    public String adminShowProducts(Model model) {

        List<Product> products = productService.getAllProductsWithVariants();

        model.addAttribute("products", products);

        return "admin/adminproduct";
    }

    // ========================================================================
    // 5. ADMIN - VIEW PRODUCT VARIANTS
    // ========================================================================
    @GetMapping("/variants")
    public String viewProductVariants(
            @RequestParam("productId") Long productId,
            Model model) {

        Product product = productService.getProductById(productId);
        if (product == null) {
            return "redirect:/admin/products";
        }

        List<ProductVariant> variants = productVariantService.getVariantsByProductId(productId);
        
        // Calculate statistics
        long totalVariants = variants.size();
        long inStockCount = variants.stream().filter(v -> v.getStock() > 20).count();
        long lowStockCount = variants.stream().filter(v -> v.getStock() > 0 && v.getStock() <= 20).count();
        long outOfStockCount = variants.stream().filter(v -> v.getStock() == 0).count();
        
        model.addAttribute("product", product);
        model.addAttribute("variants", variants);
        model.addAttribute("totalVariants", totalVariants);
        model.addAttribute("inStockCount", inStockCount);
        model.addAttribute("lowStockCount", lowStockCount);
        model.addAttribute("outOfStockCount", outOfStockCount);

        return "admin/productvariants";
    }

    // ========================================================================
    // 6. ADMIN - ADD PRODUCT
    // ========================================================================
    @GetMapping("/admin/add")
    public String showAddProductForm(Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("colors", colorRepository.findAll());
        model.addAttribute("sizes", categorySizeRepository.findAll());
        
        return "admin/addproduct";
    }

    @PostMapping("/admin/add")
    public String addProduct(
            @RequestParam("productName") String productName,
            @RequestParam("categoryId") Integer categoryId,
            @RequestParam("description") String description,
            @RequestParam("colorIds") List<Integer> colorIds,
            @RequestParam("sizeIds") List<Integer> sizeIds,
            @RequestParam("prices") List<Double> prices,
            @RequestParam("stocks") List<Integer> stocks,
            @RequestParam("variantImages") List<MultipartFile> variantImages,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        try {
            // Validate basic product information
            if (productName == null || productName.trim().isEmpty()) {
                model.addAttribute("error", "Product name is required");
                return showAddProductForm(model);
            }
            
            if (productName.trim().length() < 3) {
                model.addAttribute("error", "Product name must be at least 3 characters long");
                return showAddProductForm(model);
            }
            
            // Check if product name contains numbers
            if (productName.matches(".*\\d.*")) {
                model.addAttribute("error", "Product name cannot contain numbers");
                return showAddProductForm(model);
            }
            
            if (categoryId == null) {
                model.addAttribute("error", "Category is required");
                return showAddProductForm(model);
            }
            
            // Validate variants
            if (colorIds == null || colorIds.isEmpty()) {
                model.addAttribute("error", "At least one product variant is required");
                return showAddProductForm(model);
            }
            
            // Check if all variant arrays have the same size
            if (sizeIds == null || sizeIds.size() != colorIds.size() ||
                prices == null || prices.size() != colorIds.size() ||
                stocks == null || stocks.size() != colorIds.size() ||
                variantImages == null || variantImages.size() != colorIds.size()) {
                model.addAttribute("error", "All variant fields must be provided for each variant");
                return showAddProductForm(model);
            }
            
            // Validate each variant
            for (int i = 0; i < colorIds.size(); i++) {
                if (colorIds.get(i) == null) {
                    model.addAttribute("error", "Color is required for variant " + (i + 1));
                    return showAddProductForm(model);
                }
                if (sizeIds.get(i) == null) {
                    model.addAttribute("error", "Size is required for variant " + (i + 1));
                    return showAddProductForm(model);
                }
                if (prices.get(i) == null || prices.get(i) < 0) {
                    model.addAttribute("error", "Price must be greater than or equal to 0 for variant " + (i + 1));
                    return showAddProductForm(model);
                }
                if (stocks.get(i) == null || stocks.get(i) < 0) {
                    model.addAttribute("error", "Stock must be greater than or equal to 0 for variant " + (i + 1));
                    return showAddProductForm(model);
                }
                if (variantImages.get(i) == null || variantImages.get(i).isEmpty()) {
                    model.addAttribute("error", "Image is required for variant " + (i + 1));
                    return showAddProductForm(model);
                }
                
                // Validate image file type
                String contentType = variantImages.get(i).getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    model.addAttribute("error", "Only image files are allowed for variant " + (i + 1));
                    return showAddProductForm(model);
                }
                
                // Validate image file size (max 5MB)
                if (variantImages.get(i).getSize() > 5 * 1024 * 1024) {
                    model.addAttribute("error", "Image size must be less than 5MB for variant " + (i + 1));
                    return showAddProductForm(model);
                }
            }
            
            // Check if category exists
            Category category = categoryRepository.findById(categoryId).orElse(null);
            if (category == null) {
                model.addAttribute("error", "Selected category not found");
                return showAddProductForm(model);
            }
            
            // Create new product
            Product product = new Product();
            product.setProductName(productName.trim());
            product.setDescription(description != null ? description.trim() : "");
            product.setCategory(category);
            product.setAccountId(1L); // Set default account ID - you might want to get this from current user
            
            // Save product first
            product = productRepository.save(product);
            
            // Create product variants
            for (int i = 0; i < colorIds.size(); i++) {
                ProductVariant variant = new ProductVariant();
                variant.setProduct(product);
                
                Color color = colorRepository.findById(colorIds.get(i)).orElse(null);
                if (color == null) {
                    model.addAttribute("error", "Selected color not found for variant " + (i + 1));
                    return showAddProductForm(model);
                }
                variant.setColor(color);
                
                CategorySize size = categorySizeRepository.findById(sizeIds.get(i)).orElse(null);
                if (size == null) {
                    model.addAttribute("error", "Selected size not found for variant " + (i + 1));
                    return showAddProductForm(model);
                }
                variant.setCategorySize(size);
                
                variant.setPrice(prices.get(i));
                variant.setStock(stocks.get(i));
                
                // Upload image to Cloudinary
                String imageUrl = cloudinaryService.uploadImage(variantImages.get(i));
                if (imageUrl == null || imageUrl.trim().isEmpty()) {
                    model.addAttribute("error", "Failed to upload image for variant " + (i + 1));
                    return showAddProductForm(model);
                }
                variant.setImageUrl(imageUrl);
                
                productVariantRepository.save(variant);
            }
            
            redirectAttributes.addAttribute("success", "true");
            return "redirect:/products/admin/add";
            
        } catch (jakarta.validation.ConstraintViolationException e) {
            // Handle validation errors from entity annotations
            String errorMessage = "Validation error: ";
            for (jakarta.validation.ConstraintViolation<?> violation : e.getConstraintViolations()) {
                errorMessage += violation.getMessage() + ". ";
            }
            model.addAttribute("error", errorMessage.trim());
            return showAddProductForm(model);
        } catch (Exception e) {
            // Handle other exceptions
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("ConstraintViolationImpl")) {
                // Extract just the message from the complex error
                model.addAttribute("error", "Product name cannot contain numbers");
            } else {
                model.addAttribute("error", "Failed to add product: " + e.getMessage());
            }
            return showAddProductForm(model);
        }
    }

    // ========================================================================
    // 7. ADMIN - EDIT PRODUCT
    // ========================================================================
    @GetMapping("/admin/edit")
    public String showEditProductForm(@RequestParam("id") Long productId, Model model) {
        Product product = productService.getProductById(productId);
        if (product == null) {
            return "redirect:/admin/products";
        }
        
        model.addAttribute("product", product);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("colors", colorRepository.findAll());
        model.addAttribute("sizes", categorySizeRepository.findAll());
        model.addAttribute("variants", product.getVariants());
        
        return "admin/editproduct";
    }

    @PostMapping("/admin/edit")
    public String editProduct(
            @RequestParam("productId") Long productId,
            @RequestParam("productName") String productName,
            @RequestParam("categoryId") Integer categoryId,
            @RequestParam("description") String description,
            @RequestParam(value = "deletedVariantIds", required = false) String deletedVariantIds,
            @RequestParam(value = "colorIds", required = false) List<Integer> colorIds,
            @RequestParam(value = "sizeIds", required = false) List<Integer> sizeIds,
            @RequestParam(value = "prices", required = false) List<Double> prices,
            @RequestParam(value = "stocks", required = false) List<Integer> stocks,
            @RequestParam(value = "variantImages", required = false) List<MultipartFile> variantImages,
            RedirectAttributes redirectAttributes) {
        
        try {
            Product product = productService.getProductById(productId);
            if (product == null) {
                redirectAttributes.addAttribute("error", "Product not found");
                return "redirect:/admin/products";
            }
            
            // Update product details
            product.setProductName(productName);
            product.setDescription(description);
            
            Category category = categoryRepository.findById(categoryId).orElse(null);
            product.setCategory(category);
            
            // Save updated product
            product = productRepository.save(product);
            
            // Delete marked variants
            if (deletedVariantIds != null && !deletedVariantIds.trim().isEmpty()) {
                String[] ids = deletedVariantIds.split(",");
                for (String id : ids) {
                    try {
                        productVariantRepository.deleteById(Integer.parseInt(id.trim()));
                    } catch (Exception e) {
                        // Log error but continue
                        System.err.println("Error deleting variant: " + id);
                    }
                }
            }
            
            // Update existing variants and add new ones
            if (colorIds != null && !colorIds.isEmpty()) {
                for (int i = 0; i < colorIds.size(); i++) {
                    if (i < sizeIds.size() && i < prices.size() && i < stocks.size()) {
                        ProductVariant variant = new ProductVariant();
                        variant.setProduct(product);
                        
                        Color color = colorRepository.findById(colorIds.get(i)).orElse(null);
                        variant.setColor(color);
                        
                        CategorySize size = categorySizeRepository.findById(sizeIds.get(i)).orElse(null);
                        variant.setCategorySize(size);
                        
                        variant.setPrice(prices.get(i));
                        variant.setStock(stocks.get(i));
                        
                        // Upload image if provided
                        if (variantImages != null && i < variantImages.size() && !variantImages.get(i).isEmpty()) {
                            String imageUrl = cloudinaryService.uploadImage(variantImages.get(i));
                            variant.setImageUrl(imageUrl);
                        }
                        
                        productVariantRepository.save(variant);
                    }
                }
            }
            
            redirectAttributes.addAttribute("success", "true");
            return "redirect:/admin/products";
            
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "Failed to update product: " + e.getMessage());
            return "redirect:/products/admin/edit?id=" + productId;
        }
    }

    // ========================================================================
    // 8. ADMIN - ADD VARIANT TO PRODUCT
    // ========================================================================
    @GetMapping("/admin/variant/add")
    public String showAddVariantForm(@RequestParam("productId") Long productId, Model model) {
        Product product = productService.getProductById(productId);
        if (product == null) {
            return "redirect:/admin/products";
        }
        
        model.addAttribute("product", product);
        model.addAttribute("colors", colorRepository.findAll());
        model.addAttribute("sizes", categorySizeRepository.findAll());
        
        return "admin/addvariant";
    }

    @PostMapping("/admin/variant/add")
    public String addVariant(
            @RequestParam("productId") Long productId,
            @RequestParam("colorId") Integer colorId,
            @RequestParam("sizeId") Integer sizeId,
            @RequestParam("price") Double price,
            @RequestParam("stock") Integer stock,
            @RequestParam("variantImage") MultipartFile variantImage,
            RedirectAttributes redirectAttributes) {
        
        try {
            Product product = productService.getProductById(productId);
            if (product == null) {
                redirectAttributes.addAttribute("error", "Product not found");
                return "redirect:/admin/products";
            }
            
            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            
            Color color = colorRepository.findById(colorId).orElse(null);
            variant.setColor(color);
            
            CategorySize size = categorySizeRepository.findById(sizeId).orElse(null);
            variant.setCategorySize(size);
            
            variant.setPrice(price);
            variant.setStock(stock);
            
            // Upload image to Cloudinary
            if (!variantImage.isEmpty()) {
                String imageUrl = cloudinaryService.uploadImage(variantImage);
                variant.setImageUrl(imageUrl);
            }
            
            productVariantRepository.save(variant);
            
            redirectAttributes.addFlashAttribute("success", "Thêm biến thể thành công!");
            return "redirect:/products/variants?productId=" + productId;
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to add variant: " + e.getMessage());
            return "redirect:/products/admin/variant/add?productId=" + productId;
        }
    }

    // ========================================================================
    // 9. ADMIN - EDIT VARIANT
    // ========================================================================
    @GetMapping("/admin/variant/edit")
    public String showEditVariantForm(@RequestParam("variantId") Integer variantId, Model model) {
        ProductVariant variant = productVariantRepository.findById(variantId).orElse(null);
        if (variant == null) {
            return "redirect:/admin/products";
        }
        
        model.addAttribute("variant", variant);
        model.addAttribute("product", variant.getProduct());
        model.addAttribute("colors", colorRepository.findAll());
        model.addAttribute("sizes", categorySizeRepository.findAll());
        
        return "admin/editvariant";
    }

    @PostMapping("/admin/variant/edit")
    public String editVariant(
            @RequestParam("variantId") Integer variantId,
            @RequestParam("colorId") Integer colorId,
            @RequestParam("sizeId") Integer sizeId,
            @RequestParam("price") Double price,
            @RequestParam("stock") Integer stock,
            @RequestParam(value = "variantImage", required = false) MultipartFile variantImage,
            RedirectAttributes redirectAttributes) {
        
        try {
            ProductVariant variant = productVariantRepository.findById(variantId).orElse(null);
            if (variant == null) {
                redirectAttributes.addAttribute("error", "Variant not found");
                return "redirect:/admin/products";
            }
            
            Color color = colorRepository.findById(colorId).orElse(null);
            variant.setColor(color);
            
            CategorySize size = categorySizeRepository.findById(sizeId).orElse(null);
            variant.setCategorySize(size);
            
            variant.setPrice(price);
            variant.setStock(stock);
            
            // Upload new image if provided
            if (variantImage != null && !variantImage.isEmpty()) {
                String imageUrl = cloudinaryService.uploadImage(variantImage);
                variant.setImageUrl(imageUrl);
            }
            
            productVariantRepository.save(variant);
            
            redirectAttributes.addFlashAttribute("success", "Cập nhật biến thể thành công!");
            // Get productId from variant to redirect to product variants page
            Long productId = variant.getProduct().getProductId();
            return "redirect:/products/variants?productId=" + productId;
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update variant: " + e.getMessage());
            return "redirect:/products/admin/variant/edit?variantId=" + variantId;
        }
    }

    // ========================================================================
    // 10. ADMIN - DELETE VARIANT
    // ========================================================================
    @GetMapping("/admin/variant/delete")
    public String deleteVariant(
            @RequestParam("variantId") Integer variantId,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Get variant info to obtain productId before deletion
            ProductVariant variant = productVariantRepository.findById(variantId).orElse(null);
            
            if (variant == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy biến thể sản phẩm");
                return "redirect:/admin/products";
            }
            
            // Check if variant is linked to orders
            boolean isLinkedToOrders = orderItemRepository.existsByProductVariantVariantId(variantId);
            if (isLinkedToOrders) {
                redirectAttributes.addFlashAttribute("error", "Không thể xóa biến thể: Biến thể này đã được sử dụng trong đơn hàng");
                return "redirect:/products/variants?productId=" + variant.getProduct().getProductId();
            }
            
            // Delete the variant
            productVariantRepository.deleteById(variantId);
            redirectAttributes.addFlashAttribute("success", "Xóa biến thể sản phẩm thành công");
            
            // Redirect to product variants page
            return "redirect:/products/variants?productId=" + variant.getProduct().getProductId();
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa biến thể: " + e.getMessage());
            return "redirect:/admin/products";
        }
    }

    // ========================================================================
    // 11. ADMIN - DELETE PRODUCT
    // ========================================================================
    @PostMapping("/admin/delete")
    public String deleteProduct(
            @RequestParam("productId") Long productId,
            RedirectAttributes redirectAttributes) {
        
        try {
            boolean success = productService.deleteProduct(productId);
            if (success) {
                redirectAttributes.addFlashAttribute("success", "Xóa sản phẩm thành công!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy sản phẩm");
            }
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa sản phẩm: " + e.getMessage());
        }
        
        return "redirect:/admin/products";
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
