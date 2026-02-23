package vn.edu.fpt.fashionstore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.fashionstore.entity.Category;
import vn.edu.fpt.fashionstore.entity.Product;
import vn.edu.fpt.fashionstore.entity.ProductVariant;
import vn.edu.fpt.fashionstore.service.ProductService;

import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    // =========================
    // LIST PRODUCT
    // =========================
    @GetMapping
    public String showProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "productName") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int pageSize,
            Model model) {

        // Sort
        Sort.Direction sortDirection =
                direction.equalsIgnoreCase("desc")
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(sortDirection, sort));

        Page<Product> productPage;

        // Có filter/search thì gọi search
        if (hasFilter(keyword, categoryId, size, color, stockStatus, minPrice, maxPrice)) {
            productPage = productService.searchAndFilterProducts(
                    keyword, categoryId, size, color, stockStatus, minPrice, maxPrice, pageable
            );
        } else {
            productPage = productService.getAllProducts(pageable);
        }

        // Nếu page vượt quá total page → quay về page 0
        if (page > 0 && productPage.isEmpty() && productPage.getTotalElements() > 0) {
            pageable = PageRequest.of(0, pageSize, Sort.by(sortDirection, sort));
            if (hasFilter(keyword, categoryId, size, color, stockStatus, minPrice, maxPrice)) {
                productPage = productService.searchAndFilterProducts(
                        keyword, categoryId, size, color, stockStatus, minPrice, maxPrice, pageable
                );
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
        model.addAttribute("selectedColor", color);
        model.addAttribute("stockStatus", stockStatus);
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
    @Transactional(readOnly = true)
    public String productDetails(
            @RequestParam("id") Long productId,
            @RequestParam(required = false) String selectedSize,
            @RequestParam(required = false) String selectedColor,
            @RequestParam(required = false) Integer quantity,
            Model model) {

        Product product = productService.getProductById(productId);

        if (product == null) {
            return "redirect:/products";
        }

        model.addAttribute("product", product);
        model.addAttribute("variants", product.getVariants());
        model.addAttribute("quantity", quantity != null ? quantity : 1);
        
        // Debug: Print variants info
        System.out.println("=== DEBUG PRODUCT DETAILS ===");
        System.out.println("Product ID: " + productId);
        System.out.println("Product Name: " + product.getProductName());
        System.out.println("Category: " + (product.getCategory() != null ? product.getCategory().getCategoryName() : "NULL"));
        System.out.println("Number of variants: " + product.getVariants().size());
        
        // Find selected variant based on size and color
        if (selectedSize != null && selectedColor != null) {
            System.out.println("Looking for variant - Size: " + selectedSize + ", Color: " + selectedColor);
            for (ProductVariant variant : product.getVariants()) {
                System.out.println("Variant ID: " + variant.getVariantId());
                System.out.println("  CategorySize: " + (variant.getCategorySize() != null ? variant.getCategorySize().getSizeName() : "NULL"));
                System.out.println("  Color: " + (variant.getColor() != null ? variant.getColor().getColorName() : "NULL"));
                System.out.println("  Price: " + variant.getPrice());
                System.out.println("  Stock: " + variant.getStock());
                System.out.println("  Image URL: " + variant.getImageUrl());
                
                if (variant.getCategorySize() != null && variant.getColor() != null &&
                    variant.getCategorySize().getSizeName().equals(selectedSize) &&
                    variant.getColor().getColorName().equals(selectedColor)) {
                    model.addAttribute("selectedVariant", variant);
                    System.out.println("Found matching variant!");
                    break;
                }
            }
        } else {
            System.out.println("No selectedSize or selectedColor provided");
            // If no selection, use first variant as default
            if (!product.getVariants().isEmpty()) {
                ProductVariant firstVariant = product.getVariants().get(0);
                model.addAttribute("selectedVariant", firstVariant);
                System.out.println("Using first variant as default");
                System.out.println("Default Image URL: " + firstVariant.getImageUrl());
            }
        }

        return "productdetails";
    }

    // =========================
    // CHECK FILTER
    // =========================
    private boolean hasFilter(String keyword, Long categoryId, String size, String color, String stockStatus,
                              Double minPrice, Double maxPrice) {

        return (keyword != null && !keyword.isBlank())
                || categoryId != null
                || (size != null && !size.isBlank())
                || (color != null && !color.isBlank())
                || (stockStatus != null && !stockStatus.isBlank())
                || minPrice != null
                || maxPrice != null;
    }
}
