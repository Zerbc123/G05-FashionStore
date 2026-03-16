package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.service.AccountService;
import vn.edu.fpt.fashionstore.service.CloudinaryService;
import vn.edu.fpt.fashionstore.service.ProductService;
import vn.edu.fpt.fashionstore.entity.Category;
import vn.edu.fpt.fashionstore.service.CategoryService;
import vn.edu.fpt.fashionstore.entity.Product;
import vn.edu.fpt.fashionstore.entity.ProductVariant;
import vn.edu.fpt.fashionstore.entity.Color;
import vn.edu.fpt.fashionstore.entity.CategorySize;
import vn.edu.fpt.fashionstore.service.ProductVariantService;

import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final AccountService accountService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final ProductVariantService productVariantService;
    private final CloudinaryService cloudinaryService;

    private boolean isStaff(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        return "Staff".equals(role) || "Admin".equals(role);
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
        if (!isStaff(session)) return "redirect:/login";
        model.addAttribute("title", "Staff Dashboard");
        return "staff/staffdashboard";
    }

    @GetMapping("/support")
    public String support(HttpSession session, Model model) {
        if (!isStaff(session)) return "redirect:/login";
        model.addAttribute("title", "Customer Support");
        return "staff/staffsupport";
    }

    @GetMapping("/inventory")
    public String inventory(HttpSession session, Model model) {
        if (!isStaff(session)) return "redirect:/login";
        model.addAttribute("title", "Inventory Management");
        return "staff/inventory";
    }

    // ======== STAFF PROFILE ========

    @GetMapping("/profile")
    public String staffProfile(HttpSession session, Model model) {
        if (!isStaff(session)) return "redirect:/login";

        String email = (String) session.getAttribute("user");
        if (email == null) return "redirect:/login";

        var staff = accountService.getAccountByEmail(email);

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
        var products = productVariantService.getAllProducts();

        // Tính toán thống kê
        long totalProducts = products.size();
        long inStockCount = 0;
        long lowStockCount = 0;
        long outOfStockCount = 0;
        
        for (var product : products) {
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
