package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.fashionstore.service.AccountService;
import vn.edu.fpt.fashionstore.service.CloudinaryService;
import vn.edu.fpt.fashionstore.service.ProductService;
import vn.edu.fpt.fashionstore.entity.Category;
import vn.edu.fpt.fashionstore.service.CategoryService;
import vn.edu.fpt.fashionstore.entity.Product;
import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.entity.ProductVariant;
import vn.edu.fpt.fashionstore.entity.Color;
import vn.edu.fpt.fashionstore.entity.CategorySize;
import vn.edu.fpt.fashionstore.service.ProductVariantService;

import java.util.List;
import java.util.Objects;
import vn.edu.fpt.fashionstore.repository.AccountRepository;
import vn.edu.fpt.fashionstore.service.InventoryService;
import vn.edu.fpt.fashionstore.util.RoleUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final AccountService accountService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final ProductVariantService productVariantService;
    private final CloudinaryService cloudinaryService;
    private static final Logger logger = LoggerFactory.getLogger(StaffController.class);

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private InventoryService inventoryService;

    private boolean isStaff(HttpSession session) {
        String role = (String) session.getAttribute("userRole");

        if (role == null) {
            return false;
        }

        return "Admin".equalsIgnoreCase(role)
                || role.contains("Nhân viên bán hàng (Sale)")
                || role.contains("Quản lý kho (Stock)")
                || role.contains("Hỗ trợ khách hàng (Support)")
                || role.contains("Quản lý cửa hàng (Manager)");
    }

    private boolean isSupport(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        return role != null && role.contains("Hỗ trợ khách hàng (Support)");
    }

    private boolean isSale(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        return role != null && role.contains("Nhân viên bán hàng (Sale)");
    }

    private boolean isStock(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        return role != null && role.contains("Quản lý kho (Stock)");
    }

    // ======== STAFF VIEW ========

    @GetMapping({"", "/", "/view"})
    public String staffView(HttpSession session, Model model) {
        if (!isStaff(session)) return "redirect:/login";
        model.addAttribute("title", "Staff Panel");
        return "staff/staffview";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!RoleUtils.canAccessDashboard(session)) {
            model.addAttribute("error", RoleUtils.getAccessDeniedMessage("dashboard"));
            return "staff/access_denied";
        }
        
        // Add permission attributes for UI control
        boolean canManage = RoleUtils.canManageDashboard(session);
        model.addAttribute("canManageDashboard", canManage);
        
        model.addAttribute("title", "Staff Dashboard");
        return "staff/staffdashboard";
    }

    // Quản lý đơn hàng (Confirm Orders)
    @GetMapping("/orders")
    public String orders(HttpSession session, Model model) {
        if (!RoleUtils.canViewOrders(session)) {
            return "redirect:/login";
        }

        // Check if user can manage orders (Admin, Sale, Manager)
        boolean canManage = RoleUtils.canManageOrders(session);
        model.addAttribute("canManageOrders", canManage);

        model.addAttribute("title", "Order Management");
        return "admin/adminorder";
    }

    @GetMapping("/inventory")
    public String inventory(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             HttpSession session, Model model) {
        if (!RoleUtils.canViewInventory(session)) {
            return "redirect:/login";
        }
        
        // Check if user can manage inventory (Admin, Stock)
        boolean canManage = RoleUtils.canManageInventory(session);
        model.addAttribute("canManageInventory", canManage);
        
        // Fetch paginated data from database
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductVariant> productVariantPage = inventoryService.getAllProductVariants(pageable);
        InventoryService.InventoryStats stats = inventoryService.getInventoryStats();
        
        // Get all categories for dropdown
        List<String> categories = inventoryService.getAllCategories();
        
        model.addAttribute("title", "Inventory Management");
        model.addAttribute("productVariantPage", productVariantPage);
        model.addAttribute("productVariants", productVariantPage.getContent());
        model.addAttribute("stats", stats);
        model.addAttribute("categories", categories);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productVariantPage.getTotalPages());
        model.addAttribute("totalItems", productVariantPage.getTotalElements());
        model.addAttribute("pageSize", size);
        
        return "staff/inventory";
    }

    @PostMapping("/inventory/update-stock")
    public String updateStock(@RequestParam Integer variantId, 
                            @RequestParam Integer newStock,
                            HttpSession session, 
                            RedirectAttributes redirectAttributes) {
        if (!RoleUtils.canManageInventory(session)) {
            redirectAttributes.addFlashAttribute("error", RoleUtils.getAccessDeniedMessage("inventory"));
            return "redirect:/staff/inventory";
        }
        
        try {
            inventoryService.updateStock(variantId, newStock);
            redirectAttributes.addFlashAttribute("success", "Cập nhật tồn kho thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi cập nhật tồn kho: " + e.getMessage());
        }
        
        return "redirect:/staff/inventory";
    }

    @GetMapping("/inventory/filter")
    public String filterInventory(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 @RequestParam(required = false) String search,
                                 @RequestParam(required = false) String category,
                                 @RequestParam(required = false) String stockStatus,
                                 HttpSession session, Model model) {
        logger.info("Filter inventory called with params: page={}, size={}, search='{}', category='{}', stockStatus='{}'", 
                   page, size, search, category, stockStatus);
        
        if (!isStaff(session)) {
            logger.warn("User not authenticated as staff, redirecting to login");
            return "redirect:/login";
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductVariant> productVariantPage;
        
        try {
            // Get all variants first
            List<ProductVariant> allVariants = inventoryService.getAllProductVariants();
            
            // Apply filters sequentially
            List<ProductVariant> filteredVariants = allVariants;
            
            // Apply search filter if provided
            if (search != null && !search.trim().isEmpty()) {
                logger.info("Applying search filter for: '{}'", search.trim());
                String searchTrim = search.trim().toLowerCase();
                filteredVariants = filteredVariants.stream()
                    .filter(variant -> variant.getProduct() != null &&
                        variant.getProduct().getProductName() != null &&
                        (variant.getProduct().getProductName().toLowerCase().contains(searchTrim) ||
                         (variant.getProduct().getDescription() != null &&
                          variant.getProduct().getDescription().toLowerCase().contains(searchTrim))))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // Apply category filter if provided
            if (category != null && !category.equals("all")) {
                logger.info("Applying category filter for: '{}'", category);
                final String categoryFilter = category;
                filteredVariants = filteredVariants.stream()
                    .filter(variant -> variant.getProduct() != null && 
                        variant.getProduct().getCategory() != null &&
                        variant.getProduct().getCategory().getCategoryName() != null &&
                        variant.getProduct().getCategory().getCategoryName().equalsIgnoreCase(categoryFilter))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // Apply stock status filter if provided
            if (stockStatus != null && !stockStatus.equals("all")) {
                logger.info("Applying stock status filter for: '{}'", stockStatus);
                final String stockStatusFilter = stockStatus;
                filteredVariants = filteredVariants.stream()
                    .filter(variant -> {
                        if (variant.getStock() == null) return false;
                        switch (stockStatusFilter.toLowerCase()) {
                            case "in-stock":
                                return variant.getStock() > 20;
                            case "low-stock":
                                return variant.getStock() > 0 && variant.getStock() <= 20;
                            case "out-stock":
                                return variant.getStock() == 0;
                            default:
                                return true;
                        }
                    })
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // Apply pagination to filtered results
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filteredVariants.size());
            List<ProductVariant> pageContent = start < filteredVariants.size() ? 
                filteredVariants.subList(start, end) : java.util.Collections.emptyList();
            
            productVariantPage = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, filteredVariants.size());
            
        } catch (Exception e) {
            logger.error("Error filtering inventory: {}", e.getMessage(), e);
            // Fallback to all products if filtering fails
            productVariantPage = inventoryService.getAllProductVariants(pageable);
        }
        
        InventoryService.InventoryStats stats = inventoryService.getInventoryStats();
        
        // Get all categories for dropdown
        List<String> categories = inventoryService.getAllCategories();
        
        model.addAttribute("title", "Inventory Management");
        model.addAttribute("productVariantPage", productVariantPage);
        model.addAttribute("productVariants", productVariantPage.getContent());
        model.addAttribute("stats", stats);
        model.addAttribute("categories", categories);
        model.addAttribute("search", search);
        model.addAttribute("category", category);
        model.addAttribute("stockStatus", stockStatus);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productVariantPage.getTotalPages());
        model.addAttribute("totalItems", productVariantPage.getTotalElements());
        model.addAttribute("pageSize", size);
        
        logger.info("Returning inventory view with {} items, total pages: {}", 
                   productVariantPage.getContent().size(), productVariantPage.getTotalPages());
        
        return "staff/inventory";
    }

    // ======== STAFF PROFILE ========

    @GetMapping("/profile")
    public String staffProfile(HttpSession session, Model model) {
        if (!isStaff(session)) return "redirect:/login";

        String email = (String) session.getAttribute("user");
        if (email == null) return "redirect:/login";

        Account staff = accountService.getAccountByEmail(email);

        if (staff == null) {
            model.addAttribute("error", "Không tìm thấy thông tin nhân viên!");
            return "staff/staff_profile";
        }

        model.addAttribute("title", "Staff Profile");
        model.addAttribute("staff", staff);

        return "staff/staff_profile";
    }

    // ======== STAFF PRODUCT MANAGEMENT ========

    @GetMapping("/products")
    public String products(HttpSession session, Model model,
                          @RequestParam(required = false) String search,
                          @RequestParam(required = false) String category) {
        if (!isStaff(session)) {
            return "redirect:/login";
        }

        // Lấy danh sách sản phẩm từ database
        List<Product> products = productVariantService.getAllProducts();

        // Tính toán thống kê
        long totalProducts = products.size();
        long inStockCount = 0;
        long lowStockCount = 0;
        long outOfStockCount = 0;
        
        for (Product product : products) {
            if (product.getVariants() != null && !product.getVariants().isEmpty()) {
                int stock = product.getVariants().get(0).getStock();
                if (stock > 20) {
                    inStockCount++;
                } else if (stock > 0) {
                    lowStockCount++;
                } else {
                    outOfStockCount++;
                }
            } else {
                // Không có variant = hết hàng
                outOfStockCount++;
            }
        }
        
        model.addAttribute("products", products);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("inStockCount", inStockCount);
        model.addAttribute("lowStockCount", lowStockCount);
        model.addAttribute("outOfStockCount", outOfStockCount);
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("selectedCategory", category != null ? category : "Tất cả");
        model.addAttribute("title", "Product Management");
        
        return "staff/staffproduct";
    }

    @GetMapping("/products/add")
    public String addProduct(HttpSession session, Model model) {
        if (!isStaff(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Add New Product");
        return "staff/addproduct";
    }

    @GetMapping("/products/edit")
    public String editProduct(@RequestParam("id") Long productId, HttpSession session, Model model) {
        if (!isStaff(session)) {
            return "redirect:/login";
        }
        // Redirect to ProductController's edit endpoint
        return "redirect:/products/staff/edit?id=" + productId;
    }

    // ======== STAFF PRODUCT VARIANT MANAGEMENT ========

    @GetMapping("/products/staff/variants")
    public String viewProductVariants(
            @RequestParam("productId") Long productId,
            HttpSession session, Model model) {
        
        if (!isStaff(session)) {
            return "redirect:/login";
        }

        Product product = productService.getProductById(productId);
        if (product == null) {
            return "redirect:/staff/products";
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

        return "staff/productvariants";
    }

    @GetMapping("/products/staff/variant/add")
    public String showAddVariantForm(@RequestParam("productId") Long productId, HttpSession session, Model model) {
        if (!isStaff(session)) {
            return "redirect:/login";
        }
        
        Product product = productService.getProductById(productId);
        if (product == null) {
            return "redirect:/staff/products";
        }

        // Get available colors and sizes
        List<Color> colors = productVariantService.getAllColors();
        List<CategorySize> sizes = productVariantService.getAllCategorySizes();
        
        model.addAttribute("product", product);
        model.addAttribute("colors", colors);
        model.addAttribute("sizes", sizes);
        model.addAttribute("title", "Add New Product Variant");

        return "staff/addvariant";
    }

    @PostMapping("/products/staff/variant/add")
    public String addVariant(
            @RequestParam("productId") Long productId,
            @RequestParam("colorId") Integer colorId,
            @RequestParam("sizeId") Integer sizeId,
            @RequestParam("price") Double price,
            @RequestParam("stock") Integer stock,
            @RequestParam("variantImage") MultipartFile variantImage,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        
        if (!isStaff(session)) {
            return "redirect:/login";
        }

        try {
            // Validate input
            if (colorId == null || sizeId == null || price == null || stock == null) {
                redirectAttributes.addFlashAttribute("error", "All fields are required");
                return "redirect:/products/staff/variant/add?productId=" + productId;
            }

            if (price < 0 || stock < 0) {
                redirectAttributes.addFlashAttribute("error", "Price and stock must be greater than or equal to 0");
                return "redirect:/products/staff/variant/add?productId=" + productId;
            }

            // Handle image upload
            String imageUrl = null;
            if (variantImage != null && !variantImage.isEmpty()) {
                // Validate image file type
                String contentType = variantImage.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    redirectAttributes.addFlashAttribute("error", "Only image files are allowed");
                    return "redirect:/products/staff/variant/add?productId=" + productId;
                }

                // Upload image to Cloudinary
                imageUrl = cloudinaryService.uploadImage(variantImage);
            }

            // Create new variant
            ProductVariant variant = new ProductVariant();
            variant.setProduct(productService.getProductById(productId));
            
            // Find color and size by ID using proper comparison
            variant.setColor(productVariantService.getAllColors().stream()
                .filter(c -> Objects.equals(c.getColorId(), colorId))
                .findFirst().orElse(null));
            variant.setCategorySize(productVariantService.getAllCategorySizes().stream()
                .filter(s -> Objects.equals(s.getCategorySizeId(), sizeId))
                .findFirst().orElse(null));
            
            variant.setPrice(price);
            variant.setStock(stock);
            variant.setImageUrl(imageUrl);

            productVariantService.createVariant(variant);

            redirectAttributes.addFlashAttribute("success", "Product variant added successfully!");
            return "redirect:/products/staff/variants?productId=" + productId;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to add variant: " + e.getMessage());
            return "redirect:/products/staff/variant/add?productId=" + productId;
        }
    }

    @GetMapping("/products/staff/variant/edit")
    public String showEditVariantForm(@RequestParam("variantId") Integer variantId, HttpSession session, Model model) {
        if (!isStaff(session)) {
            return "redirect:/login";
        }
        
        ProductVariant variant = productVariantService.getVariantById(variantId).orElse(null);
        if (variant == null) {
            return "redirect:/staff/products";
        }

        // Get available colors and sizes
        List<Color> colors = productVariantService.getAllColors();
        List<CategorySize> sizes = productVariantService.getAllCategorySizes();
        
        model.addAttribute("variant", variant);
        model.addAttribute("colors", colors);
        model.addAttribute("sizes", sizes);
        model.addAttribute("title", "Edit Product Variant");

        return "staff/editvariant";
    }

    @PostMapping("/products/staff/variant/edit")
    public String editVariant(
            @RequestParam("variantId") Integer variantId,
            @RequestParam("colorId") Integer colorId,
            @RequestParam("sizeId") Integer sizeId,
            @RequestParam("price") Double price,
            @RequestParam("stock") Integer stock,
            @RequestParam("variantImage") MultipartFile variantImage,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        
        if (!isStaff(session)) {
            return "redirect:/login";
        }

        try {
            // Validate input
            if (colorId == null || sizeId == null || price == null || stock == null) {
                redirectAttributes.addFlashAttribute("error", "All fields are required");
                return "redirect:/products/staff/variant/edit?variantId=" + variantId;
            }

            if (price < 0 || stock < 0) {
                redirectAttributes.addFlashAttribute("error", "Price and stock must be greater than or equal to 0");
                return "redirect:/products/staff/variant/edit?variantId=" + variantId;
            }

            // Get existing variant
            ProductVariant variant = productVariantService.getVariantById(variantId).orElse(null);
            if (variant == null) {
                redirectAttributes.addFlashAttribute("error", "Variant not found");
                return "redirect:/staff/products";
            }

            // Handle image upload
            if (variantImage != null && !variantImage.isEmpty()) {
                // Validate image file type
                String contentType = variantImage.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    redirectAttributes.addFlashAttribute("error", "Only image files are allowed");
                    return "redirect:/products/staff/variant/edit?variantId=" + variantId;
                }

                // Upload new image to Cloudinary
                String newImageUrl = cloudinaryService.uploadImage(variantImage);
                variant.setImageUrl(newImageUrl);
            }

            // Update variant properties
            variant.setColor(productVariantService.getAllColors().stream()
                .filter(c -> Objects.equals(c.getColorId(), colorId))
                .findFirst().orElse(null));
            variant.setCategorySize(productVariantService.getAllCategorySizes().stream()
                .filter(s -> Objects.equals(s.getCategorySizeId(), sizeId))
                .findFirst().orElse(null));
            variant.setPrice(price);
            variant.setStock(stock);

            productVariantService.updateVariant(variantId, variant);

            redirectAttributes.addFlashAttribute("success", "Product variant updated successfully!");
            return "redirect:/products/staff/variants?productId=" + variant.getProduct().getProductId();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update variant: " + e.getMessage());
            return "redirect:/products/staff/variant/edit?variantId=" + variantId;
        }
    }

    @GetMapping("/products/staff/variant/delete")
    public String deleteVariant(
            @RequestParam("variantId") Integer variantId,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        
        if (!isStaff(session)) {
            return "redirect:/login";
        }

        try {
            ProductVariant variant = productVariantService.getVariantById(variantId).orElse(null);
            if (variant == null) {
                redirectAttributes.addFlashAttribute("error", "Variant not found");
                return "redirect:/staff/products";
            }

            // Check if variant is referenced in any order items
            if (productVariantService.isVariantLinkedToOrders(variantId)) {
                redirectAttributes.addFlashAttribute("error", "Cannot delete variant: It is referenced in existing orders");
                return "redirect:/products/staff/variants?productId=" + variant.getProduct().getProductId();
            }

            productVariantService.deleteVariant(variantId);

            redirectAttributes.addFlashAttribute("success", "Product variant deleted successfully!");
            return "redirect:/products/staff/variants?productId=" + variant.getProduct().getProductId();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete variant: " + e.getMessage());
            return "redirect:/staff/products";
        }
    }

    // ======== STAFF CATEGORY MANAGEMENT ========

    @GetMapping("/categories")
    public String listCategories(HttpSession session, Model model) {
        if (!isStaff(session)) {
            return "redirect:/login";
        }
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("title", "Category Management");
        return "staff/category-list";
    }

    @GetMapping("/categories/create")
    public String createCategoryForm(HttpSession session, Model model) {
        if (!isStaff(session)) {
            return "redirect:/login";
        }
        model.addAttribute("category", new Category());
        model.addAttribute("title", "Add New Category");
        return "staff/category-form";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@ModelAttribute Category category, HttpSession session) {
        if (!isStaff(session)) {
            return "redirect:/login";
        }
        categoryService.save(category);
        return "redirect:/staff/categories";
    }

    @GetMapping("/categories/edit/{id}")
    public String editCategory(@PathVariable int id, HttpSession session, Model model) {
        if (!isStaff(session)) {
            return "redirect:/login";
        }
        model.addAttribute("category", categoryService.getById(id));
        model.addAttribute("title", "Edit Category");
        return "staff/category-form";
    }

    @PostMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable int id, HttpSession session, Model model) {
        if (!isStaff(session)) {
            return "redirect:/login";
        }

        boolean deleted = categoryService.delete(id);

        if (!deleted) {
            model.addAttribute("error",
                    "Cannot delete category because it is used by a product.");
        }

        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("title", "Category Management");

        return "staff/category-list";
    }
}
