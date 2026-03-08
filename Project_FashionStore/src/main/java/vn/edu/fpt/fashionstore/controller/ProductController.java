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

import java.util.List;
import java.util.stream.Collectors;

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
            RedirectAttributes redirectAttributes) {
        
        try {
            // Create new product
            Product product = new Product();
            product.setProductName(productName);
            product.setDescription(description);
            
            Category category = categoryRepository.findById(categoryId).orElse(null);
            product.setCategory(category);
            
            // Save product first
            product = productRepository.save(product);
            
            // Create product variants
            for (int i = 0; i < colorIds.size(); i++) {
                if (i < sizeIds.size() && i < prices.size() && i < stocks.size() && i < variantImages.size()) {
                    ProductVariant variant = new ProductVariant();
                    variant.setProduct(product);
                    
                    Color color = colorRepository.findById(colorIds.get(i)).orElse(null);
                    variant.setColor(color);
                    
                    CategorySize size = categorySizeRepository.findById(sizeIds.get(i)).orElse(null);
                    variant.setCategorySize(size);
                    
                    variant.setPrice(prices.get(i));
                    variant.setStock(stocks.get(i));
                    
                    // Upload image to Cloudinary
                    if (!variantImages.get(i).isEmpty()) {
                        String imageUrl = cloudinaryService.uploadImage(variantImages.get(i));
                        variant.setImageUrl(imageUrl);
                    }
                    
                    productVariantRepository.save(variant);
                }
            }
            
            redirectAttributes.addAttribute("success", "true");
            return "redirect:/admin/products/add";
            
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "Failed to add product: " + e.getMessage());
            return "redirect:/admin/products/add";
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
            return "redirect:/admin/product-variants";
            
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
            return "redirect:/admin/product-variants";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update variant: " + e.getMessage());
            return "redirect:/products/admin/variant/edit?variantId=" + variantId;
        }
    }
}
