package vn.edu.fpt.fashionstore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.fashionstore.entity.*;
import vn.edu.fpt.fashionstore.service.ProductService;
import vn.edu.fpt.fashionstore.service.ProductVariantService;
import vn.edu.fpt.fashionstore.service.CloudinaryService;
import vn.edu.fpt.fashionstore.repository.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductController {

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

    // =========================
    // LIST PRODUCT
    // =========================
    @GetMapping
    public String showProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            Model model) {

        List<Product> products;

        // Có filter/search thì gọi search (tạm thời chỉ dùng getAllProductsWithVariants)
        products = productService.getAllProductsWithVariants();

        // =========================
        // DATA CHO VIEW
        // =========================
        model.addAttribute("products", products);

        // giữ filter
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedSize", size);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);

        // Categories cho dropdown filter
        List<Category> categoryIds = productService.getAllCategoryIds();
        model.addAttribute("categoryIds", categoryIds);

        // Calculate statistics
        long totalProducts = products.size();
        long inStockCount = products.stream().filter(p -> p.getVariants() != null && !p.getVariants().isEmpty() && p.getVariants().get(0).getStock() > 20).count();
        long lowStockCount = products.stream().filter(p -> p.getVariants() != null && !p.getVariants().isEmpty() && p.getVariants().get(0).getStock() > 0 && p.getVariants().get(0).getStock() <= 20).count();
        long outOfStockCount = products.stream().filter(p -> p.getVariants() == null || p.getVariants().isEmpty() || p.getVariants().get(0).getStock() == 0).count();
        
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("inStockCount", inStockCount);
        model.addAttribute("lowStockCount", lowStockCount);
        model.addAttribute("outOfStockCount", outOfStockCount);

        return "admin/adminproduct";
    }

    // =========================
    // PRODUCT DETAILS
    // =========================
    @GetMapping("/detail")
    public String productDetails(
            @RequestParam("id") Long productId,
            Model model) {

        Product product = productService.getProductById(productId);

        if (product == null) {
            return "redirect:/products";
        }

        model.addAttribute("product", product);
        model.addAttribute("variants", product.getVariants());

        return "productdetails";
    }

    // =========================
    // CHECK FILTER
    // =========================
    private boolean hasFilter(String keyword, Long categoryId, String size,
            Double minPrice, Double maxPrice) {

        return (keyword != null && !keyword.isBlank())
                || categoryId != null
                || (size != null && !size.isBlank())
                || minPrice != null
                || maxPrice != null;
    }

    // =========================
    // ADMIN - LIST PRODUCT
    // =========================
    @GetMapping("/admin/products")
    public String adminShowProducts(Model model) {

        List<Product> products = productService.getAllProductsWithVariants();

        model.addAttribute("products", products);

        return "admin/adminproduct";
    }

    // =========================
    // VIEW PRODUCT VARIANTS
    // =========================
    @GetMapping("/variants")
    public String viewProductVariants(
            @RequestParam("productId") Long productId,
            Model model) {

        Product product = productService.getProductById(productId);
        if (product == null) {
            return "redirect:/products/admin/products";
        }

        List<ProductVariant> variants = productVariantService.getVariantsByProductId(productId);
        
        model.addAttribute("product", product);
        model.addAttribute("variants", variants);

        return "admin/productvariants";
    }

    // =========================
    // ADD PRODUCT
    // =========================
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
            @RequestParam("description") String description,
            @RequestParam("categoryId") Integer categoryId,
            @RequestParam(value = "variantImages", required = false) MultipartFile[] variantImages,
            @RequestParam(value = "variants", required = false) List<String> variantData,
            @RequestParam(value = "colorIds", required = false) List<Integer> colorIds,
            @RequestParam(value = "sizeIds", required = false) List<Integer> sizeIds,
            @RequestParam(value = "prices", required = false) List<Double> prices,
            @RequestParam(value = "stocks", required = false) List<Integer> stocks,
            Model model) {
        
        try {
            // Manual validation
            if (productName == null || productName.trim().isEmpty()) {
                throw new IllegalArgumentException("Product name is required");
            }
            if (categoryId == null) {
                throw new IllegalArgumentException("Category is required");
            }
            
            // Create Product and Variants directly
            Product product = new Product();
            product.setProductName(productName);
            product.setDescription(description);
            product.setAccountId(1L); // Default account ID = 1
            
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            product.setCategory(category);
            
            Product savedProduct = productRepository.save(product);
            
            // Handle variants if any
            int variantCount = 0;
            if (colorIds != null && !colorIds.isEmpty()) {
                variantCount = colorIds.size();
            } else if (variantImages != null) {
                variantCount = variantImages.length;
            }
            
            if (variantCount > 0) {
                List<ProductVariant> variants = new ArrayList<>();
                
                for (int i = 0; i < variantCount; i++) {
                    ProductVariant variant = new ProductVariant();
                    variant.setProduct(savedProduct);
                    
                    // Set color
                    if (colorIds != null && i < colorIds.size()) {
                        Color color = colorRepository.findById(colorIds.get(i))
                                .orElseThrow(() -> new RuntimeException("Color not found"));
                        variant.setColor(color);
                    }
                    
                    // Set size
                    if (sizeIds != null && i < sizeIds.size()) {
                        CategorySize size = categorySizeRepository.findById(sizeIds.get(i))
                                .orElseThrow(() -> new RuntimeException("Size not found"));
                        variant.setCategorySize(size);
                    }
                    
                    // Set price
                    if (prices != null && i < prices.size()) {
                        variant.setPrice(prices.get(i));
                    } else {
                        variant.setPrice(0.0);
                    }
                    
                    // Set stock
                    if (stocks != null && i < stocks.size()) {
                        variant.setStock(stocks.get(i));
                    } else {
                        variant.setStock(0);
                    }
                    
                    // Upload image if available
                    if (variantImages != null && i < variantImages.length && 
                        variantImages[i] != null && !variantImages[i].isEmpty()) {
                        String imageUrl = cloudinaryService.uploadImage(variantImages[i]);
                        variant.setImageUrl(imageUrl);
                    }
                    
                    variants.add(variant);
                }
                
                if (!variants.isEmpty()) {
                    productVariantRepository.saveAll(variants);
                }
            }
            
            return "redirect:/admin/products?success";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", categoryRepository.findAll());
            model.addAttribute("colors", colorRepository.findAll());
            model.addAttribute("sizes", categorySizeRepository.findAll());
            return "admin/addproduct";
        }
    }
}
