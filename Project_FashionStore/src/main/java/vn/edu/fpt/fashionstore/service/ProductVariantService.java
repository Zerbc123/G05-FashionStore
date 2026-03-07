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

    public List<ProductVariant> getAllVariants() {
        return productVariantRepository.findAll();
    }

    public Page<ProductVariant> getAllVariants(Pageable pageable) {
        return productVariantRepository.findAll(pageable);
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

    public List<Color> getAllColors() {
        return colorRepository.findAll();
    }

    public List<CategorySize> getAllCategorySizes() {
        return categorySizeRepository.findAll();
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
}
