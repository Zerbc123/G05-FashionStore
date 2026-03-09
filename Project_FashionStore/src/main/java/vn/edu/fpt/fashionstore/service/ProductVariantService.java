package vn.edu.fpt.fashionstore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.edu.fpt.fashionstore.entity.*;
import vn.edu.fpt.fashionstore.repository.*;

import java.util.List;
import java.util.Optional;

@Service
public class ProductVariantService {

    @Autowired
    private ProductVariantRepository productVariantRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ColorRepository colorRepository;
    
    @Autowired
    private CategorySizeRepository categorySizeRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;

    public List<ProductVariant> getAllVariants() {
        return productVariantRepository.findAll();
    }

    public Page<ProductVariant> getAllVariants(Pageable pageable) {
        return productVariantRepository.findAll(pageable);
    }

    public Page<ProductVariant> searchVariants(String searchTerm, Pageable pageable) {
        return productVariantRepository.searchVariants(searchTerm, pageable);
    }

    public Optional<ProductVariant> getVariantById(int variantId) {
        return productVariantRepository.findById(variantId);
    }

    public List<ProductVariant> getVariantsByProductId(Long productId) {
        return productVariantRepository.findByProduct_ProductId(productId);
    }

    public ProductVariant createVariant(ProductVariant variant) {
        return productVariantRepository.save(variant);
    }

    public ProductVariant updateVariant(int variantId, ProductVariant variantDetails) {
        Optional<ProductVariant> existingVariant = productVariantRepository.findById(variantId);
        if (existingVariant.isPresent()) {
            ProductVariant variant = existingVariant.get();
            variant.setColor(variantDetails.getColor());
            variant.setCategorySize(variantDetails.getCategorySize());
            variant.setPrice(variantDetails.getPrice());
            variant.setStock(variantDetails.getStock());
            variant.setImageUrl(variantDetails.getImageUrl());
            return productVariantRepository.save(variant);
        }
        return null;
    }

    public boolean deleteVariant(int variantId) {
        if (productVariantRepository.existsById(variantId)) {
            productVariantRepository.deleteById(variantId);
            return true;
        }
        return false;
    }

    public boolean isVariantLinkedToOrders(int variantId) {
        // Check if variant exists in any order items
        return orderItemRepository.existsByProductVariantVariantId(variantId);
    }

    public String deleteVariantWithOrderCheck(int variantId) {
        if (!productVariantRepository.existsById(variantId)) {
            return "Variant not found";
        }
        
        if (isVariantLinkedToOrders(variantId)) {
            return "Cannot delete variant: It is linked to existing orders";
        }
        
        try {
            productVariantRepository.deleteById(variantId);
            return "success";
        } catch (Exception e) {
            return "Error deleting variant: " + e.getMessage();
        }
    }

    public List<Color> getAllColors() {
        return colorRepository.findAll();
    }

    public List<CategorySize> getAllCategorySizes() {
        return categorySizeRepository.findAll();
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // Enhanced statistics methods for comprehensive product analysis
    public long getTotalProducts() {
        return productRepository.count();
    }

    public long getTotalVariants() {
        return productVariantRepository.count();
    }

    public long getInStockVariantCount() {
        return productVariantRepository.countByStockGreaterThan(20);
    }

    public long getLowStockVariantCount() {
        return productVariantRepository.countByStockBetween(1, 20);
    }

    public long getOutOfStockVariantCount() {
        return productVariantRepository.countByStockEquals(0);
    }

    public double getAveragePrice() {
        return productVariantRepository.findAll().stream()
            .filter(v -> v.getPrice() != null)
            .mapToDouble(ProductVariant::getPrice)
            .average()
            .orElse(0.0);
    }

    public long getProductsOutOfStock() {
        return productRepository.findAll().stream()
            .filter(product -> {
                List<ProductVariant> variants = productVariantRepository.findByProduct_ProductId(product.getProductId());
                return variants.isEmpty() || variants.stream().allMatch(v -> v.getStock() == 0);
            })
            .count();
    }

    public long getProductsInStock() {
        return productRepository.findAll().stream()
            .filter(product -> {
                List<ProductVariant> variants = productVariantRepository.findByProduct_ProductId(product.getProductId());
                return !variants.isEmpty() && variants.stream().anyMatch(v -> v.getStock() > 0);
            })
            .count();
    }

    public long getProductsLowStock() {
        return productRepository.findAll().stream()
            .filter(product -> {
                List<ProductVariant> variants = productVariantRepository.findByProduct_ProductId(product.getProductId());
                return !variants.isEmpty() && 
                       variants.stream().anyMatch(v -> v.getStock() > 0 && v.getStock() <= 20) &&
                       variants.stream().noneMatch(v -> v.getStock() > 20);
            })
            .count();
    }

    public long getTotalStockQuantity() {
        return productVariantRepository.findAll().stream()
            .mapToLong(v -> v.getStock() != null ? v.getStock() : 0)
            .sum();
    }

    public long getLowStockVariantThreshold() {
        return 20; // Threshold for low stock
    }
}
