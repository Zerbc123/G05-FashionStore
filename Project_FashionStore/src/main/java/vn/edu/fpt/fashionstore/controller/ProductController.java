package vn.edu.fpt.fashionstore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.fashionstore.entity.Category;
import vn.edu.fpt.fashionstore.entity.Product;
import vn.edu.fpt.fashionstore.entity.ProductVariant;
import vn.edu.fpt.fashionstore.service.ProductService;
import vn.edu.fpt.fashionstore.service.ProductVariantService;

import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductVariantService productVariantService;

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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int pageSize,
            @RequestParam(defaultValue = "productName") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {

        // Sort
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(sortDirection, sort));

        Page<Product> productPage;

        // Có filter/search thì gọi search
        if (hasFilter(keyword, categoryId, size, minPrice, maxPrice)) {
            productPage = productService.searchAndFilterProducts(
                    keyword, categoryId, size, minPrice, maxPrice, pageable);
        } else {
            productPage = productService.getAllProducts(pageable);
        }

        // Nếu page vượt quá total page → quay về page 0
        if (page > 0 && productPage.isEmpty() && productPage.getTotalElements() > 0) {
            pageable = PageRequest.of(0, pageSize, Sort.by(sortDirection, sort));
            if (hasFilter(keyword, categoryId, size, minPrice, maxPrice)) {
                productPage = productService.searchAndFilterProducts(
                        keyword, categoryId, size, minPrice, maxPrice, pageable);
            } else {
                productPage = productService.getAllProducts(pageable);
            }
        }

        // =========================
        // DATA CHO VIEW
        // =========================
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", productPage.getNumber());
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalItems", productPage.getTotalElements());
        model.addAttribute("pageSize", pageSize);

        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);

        // giữ filter
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedSize", size);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);

        // category cho filter
        List<Category> categoryIds = productService.getAllCategoryIds();
        model.addAttribute("categoryIds", categoryIds);

        // Hiển thị "Showing x – y of z"
        long total = productPage.getTotalElements();
        int start = total == 0 ? 0 : productPage.getNumber() * pageSize + 1;
        int end = Math.min(start + productPage.getNumberOfElements() - 1, (int) total);

        model.addAttribute("startItem", start);
        model.addAttribute("endItem", end);

        return "list";
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
}
