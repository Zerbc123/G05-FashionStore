package vn.edu.fpt.fashionstore.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.edu.fpt.fashionstore.entity.Product;
import vn.edu.fpt.fashionstore.entity.ProductVariant;
import vn.edu.fpt.fashionstore.repository.ProductRepository;
import vn.edu.fpt.fashionstore.repository.ProductVariantRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

    @Autowired
    private ProductVariantRepository productVariantRepository;
    
    @Autowired
    private ProductRepository productRepository;

    public List<ProductVariant> getAllProductVariants() {
        return productVariantRepository.findAll();
    }

    public Page<ProductVariant> getAllProductVariants(Pageable pageable) {
        return productVariantRepository.findAll(pageable);
    }

    public Page<ProductVariant> getAllProductVariantsWithProductAndCategory(Pageable pageable) {
        // Use JOIN FETCH to avoid lazy loading issues
        List<ProductVariant> variants = productVariantRepository.findAllWithProductAndCategory();
        
        // If pageable is null, return all variants as a page
        if (pageable == null) {
            return new PageImpl<>(variants, PageRequest.of(0, variants.size()), variants.size());
        }
        
        // Apply pagination manually since JOIN FETCH doesn't work with Pageable
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), variants.size());
        List<ProductVariant> pageContent = start < variants.size() ? 
            variants.subList(start, end) : Collections.emptyList();
        
        return new PageImpl<>(pageContent, pageable, variants.size());
    }

    // Helper method to get all variants with JOIN FETCH
    public List<ProductVariant> getAllVariantsWithProductAndCategory() {
        return productVariantRepository.findAllWithProductAndCategory();
    }

    public List<String> getAllCategories() {
        logger.info("Getting all unique categories from database");
        
        // Use JOIN FETCH to avoid lazy loading issues
        List<String> categories = productVariantRepository.findAllWithProductAndCategory().stream()
                .filter(variant -> variant.getProduct() != null && 
                        variant.getProduct().getCategory() != null &&
                        variant.getProduct().getCategory().getCategoryName() != null)
                .map(variant -> variant.getProduct().getCategory().getCategoryName())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        
        logger.info("Found {} unique categories: {}", categories.size(), categories);
        return categories;
    }

    public List<ProductVariant> getProductVariantsByCategory(String category) {
        return productVariantRepository.findAll().stream()
                .filter(variant -> variant.getProduct() != null && 
                        variant.getProduct().getCategory() != null &&
                        variant.getProduct().getCategory().getCategoryName() != null &&
                        variant.getProduct().getCategory().getCategoryName().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    public Page<ProductVariant> getProductVariantsByCategory(String category, Pageable pageable) {
        logger.info("=== getProductVariantsByCategory Debug ===");
        logger.info("Filtering by category: '{}'", category);
        
        List<ProductVariant> allVariants = productVariantRepository.findAll();
        logger.info("Total variants in database: {}", allVariants.size());
        
        List<ProductVariant> filtered = allVariants.stream()
                .filter(variant -> {
                    boolean hasProduct = variant.getProduct() != null;
                    boolean hasCategory = hasProduct && variant.getProduct().getCategory() != null;
                    boolean hasCategoryName = hasCategory && variant.getProduct().getCategory().getCategoryName() != null;
                    boolean categoryMatches = hasCategoryName && variant.getProduct().getCategory().getCategoryName().equalsIgnoreCase(category);
                    
                    if (hasCategoryName) {
                        logger.debug("Variant category: '{}' vs filter: '{}' -> match: {}", 
                                   variant.getProduct().getCategory().getCategoryName(), category, categoryMatches);
                    }
                    
                    return categoryMatches;
                })
                .collect(Collectors.toList());
        
        logger.info("Filtered variants count: {}", filtered.size());
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<ProductVariant> pageContent = filtered.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    public List<ProductVariant> getProductVariantsByStockStatus(String stockStatus) {
        return productVariantRepository.findAll().stream()
                .filter(variant -> {
                    if (variant.getStock() == null) return false;
                    switch (stockStatus.toLowerCase()) {
                        case "in-stock":
                            return variant.getStock() > 20;
                        case "low-stock":
                            return variant.getStock() > 0 && variant.getStock() <= 20;
                        case "out-stock":
                            return variant.getStock() == null || variant.getStock() == 0;
                        default:
                            return true;
                    }
                })
                .collect(Collectors.toList());
    }

    public Page<ProductVariant> getProductVariantsByStockStatus(String stockStatus, Pageable pageable) {
        List<ProductVariant> filtered = productVariantRepository.findAll().stream()
                .filter(variant -> {
                    if (variant.getStock() == null) return false;
                    switch (stockStatus.toLowerCase()) {
                        case "in-stock":
                            return variant.getStock() > 20;
                        case "low-stock":
                            return variant.getStock() > 0 && variant.getStock() <= 20;
                        case "out-stock":
                            return variant.getStock() == null || variant.getStock() == 0;
                        default:
                            return true;
                    }
                })
                .collect(Collectors.toList());
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<ProductVariant> pageContent = filtered.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    public List<ProductVariant> searchProductVariants(String searchTerm) {
        return productVariantRepository.findAll().stream()
                .filter(variant -> variant.getProduct() != null &&
                        variant.getProduct().getProductName() != null &&
                        (variant.getProduct().getProductName().toLowerCase().contains(searchTerm.toLowerCase()) ||
                         (variant.getProduct().getDescription() != null &&
                          variant.getProduct().getDescription().toLowerCase().contains(searchTerm.toLowerCase()))))
                .collect(Collectors.toList());
    }

    public Page<ProductVariant> searchProductVariants(String searchTerm, Pageable pageable) {
        List<ProductVariant> filtered = productVariantRepository.findAll().stream()
                .filter(variant -> variant.getProduct() != null &&
                        variant.getProduct().getProductName() != null &&
                        (variant.getProduct().getProductName().toLowerCase().contains(searchTerm.toLowerCase()) ||
                         (variant.getProduct().getDescription() != null &&
                          variant.getProduct().getDescription().toLowerCase().contains(searchTerm.toLowerCase()))))
                .collect(Collectors.toList());
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<ProductVariant> pageContent = filtered.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    public ProductVariant updateStock(Integer variantId, Integer newStock) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Product variant not found with id: " + variantId));
        variant.setStock(newStock);
        return productVariantRepository.save(variant);
    }

    public InventoryStats getInventoryStats() {
        List<ProductVariant> allVariants = productVariantRepository.findAll();
        
        // Tính tổng số lượng sản phẩm (tổng stock của tất cả variants)
        long totalProducts = allVariants.stream()
                .filter(variant -> variant.getStock() != null)
                .mapToLong(ProductVariant::getStock)
                .sum();
        
        // Tính tổng stock của các variants còn hàng (stock > 20)
        long inStockCount = allVariants.stream()
                .filter(variant -> variant.getStock() != null && variant.getStock() > 20)
                .mapToLong(ProductVariant::getStock)
                .sum();
        
        // Tính tổng stock của các variants sắp hết (0 < stock <= 20)
        long lowStockCount = allVariants.stream()
                .filter(variant -> variant.getStock() != null && variant.getStock() > 0 && variant.getStock() <= 20)
                .mapToLong(ProductVariant::getStock)
                .sum();
        
        // Đếm số variants hết hàng (stock = 0)
        long outOfStockCount = allVariants.stream()
                .filter(variant -> variant.getStock() != null && variant.getStock() == 0)
                .count();

        return new InventoryStats(totalProducts, inStockCount, lowStockCount, outOfStockCount);
    }

    // Method to get products grouped with inventory info
    public Page<ProductInventoryDTO> getAllProductsGrouped(Pageable pageable) {
        List<Product> allProducts = productRepository.findAll();
        
        List<ProductInventoryDTO> productInventoryList = allProducts.stream()
                .map(product -> {
                    List<ProductVariant> variants = productVariantRepository.findByProduct_ProductId(product.getProductId());
                    
                    // Calculate total stock
                    int totalStock = variants.stream()
                            .filter(v -> v.getStock() != null)
                            .mapToInt(ProductVariant::getStock)
                            .sum();
                    
                    // Get min price
                    Double minPrice = variants.stream()
                            .filter(v -> v.getPrice() != null)
                            .mapToDouble(ProductVariant::getPrice)
                            .min()
                            .orElse(0.0);
                    
                    // Determine status
                    String status;
                    if (totalStock == 0) {
                        status = "out-stock";
                    } else if (totalStock <= 20) {
                        status = "low-stock";
                    } else {
                        status = "in-stock";
                    }
                    
                    // Get first variant image
                    String imageUrl = variants.stream()
                            .filter(v -> v.getImageUrl() != null && !v.getImageUrl().isEmpty())
                            .map(ProductVariant::getImageUrl)
                            .findFirst()
                            .orElse(null);
                    
                    return new ProductInventoryDTO(
                            product.getProductId(),
                            product.getProductName(),
                            product.getCategory() != null ? product.getCategory().getCategoryName() : null,
                            minPrice,
                            totalStock,
                            variants.size(),
                            status,
                            imageUrl
                    );
                })
                .collect(Collectors.toList());
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), productInventoryList.size());
        List<ProductInventoryDTO> pageContent = start < productInventoryList.size() ? 
            productInventoryList.subList(start, end) : Collections.emptyList();
        
        return new PageImpl<>(pageContent, pageable, productInventoryList.size());
    }

    public static class InventoryStats {
        private long totalProducts;
        private long inStockCount;
        private long lowStockCount;
        private long outOfStockCount;

        public InventoryStats(long totalProducts, long inStockCount, long lowStockCount, long outOfStockCount) {
            this.totalProducts = totalProducts;
            this.inStockCount = inStockCount;
            this.lowStockCount = lowStockCount;
            this.outOfStockCount = outOfStockCount;
        }

        // Getters
        public long getTotalProducts() { return totalProducts; }
        public long getInStockCount() { return inStockCount; }
        public long getLowStockCount() { return lowStockCount; }
        public long getOutOfStockCount() { return outOfStockCount; }
    }
    
    // DTO for product inventory display
    public static class ProductInventoryDTO {
        private Long productId;
        private String productName;
        private String categoryName;
        private Double minPrice;
        private Integer totalStock;
        private Integer variantCount;
        private String status;
        private String imageUrl;
        
        public ProductInventoryDTO(Long productId, String productName, String categoryName, 
                                  Double minPrice, Integer totalStock, Integer variantCount, 
                                  String status, String imageUrl) {
            this.productId = productId;
            this.productName = productName;
            this.categoryName = categoryName;
            this.minPrice = minPrice;
            this.totalStock = totalStock;
            this.variantCount = variantCount;
            this.status = status;
            this.imageUrl = imageUrl;
        }
        
        // Getters
        public Long getProductId() { return productId; }
        public String getProductName() { return productName; }
        public String getCategoryName() { return categoryName; }
        public Double getMinPrice() { return minPrice; }
        public Integer getTotalStock() { return totalStock; }
        public Integer getVariantCount() { return variantCount; }
        public String getStatus() { return status; }
        public String getImageUrl() { return imageUrl; }
    }
}
