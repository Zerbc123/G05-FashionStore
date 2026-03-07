package vn.edu.fpt.fashionstore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.fashionstore.dto.ProductAddRequest;
import vn.edu.fpt.fashionstore.dto.ProductVariantRequest;
import vn.edu.fpt.fashionstore.entity.*;
import vn.edu.fpt.fashionstore.repository.*;

import jakarta.persistence.criteria.Predicate;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ProductVariantRepository productVariantRepository;
    
    @Autowired
    private CategoriesRepository categoryRepository;
    
    @Autowired
    private ColorRepository colorRepository;
    
    @Autowired
    private CategorySizeRepository categorySizeRepository;
    
    @Autowired
    private CloudinaryService cloudinaryService;

    // Lấy tất cả sản phẩm với phân trang
    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    // Tìm kiếm sản phẩm theo tên
    public Page<Product> searchProducts(String keyword, Pageable pageable) {
        Specification<Product> spec = (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("productName")),
                    "%" + keyword.toLowerCase() + "%");
        };
        return productRepository.findAll(spec, pageable);
    }

    // Lọc sản phẩm theo nhiều tiêu chí
    public Page<Product> filterProducts(Long categoryId, String size, Double minPrice, Double maxPrice,
            Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            java.util.List<Predicate> predicates = new java.util.ArrayList<>();

            // Lọc theo danh mục
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }

            // Lọc theo size (nếu có field size trong entity)
            if (size != null && !size.trim().isEmpty()) {
                // predicates.add(cb.equal(root.get("size"), size));
                // Tạm thời bỏ qua vì entity Product chưa có field size
            }

            // Lọc theo giá tối thiểu (nếu có field price trong entity)
            if (minPrice != null) {
                // predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
                // Tạm thời bỏ qua vì entity Product chưa có field price
            }

            // Lọc theo giá tối đa (nếu có field price trong entity)
            if (maxPrice != null) {
                // predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
                // Tạm thời bỏ qua vì entity Product chưa có field price
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return productRepository.findAll(spec, pageable);
    }

    // Tìm kiếm và lọc kết hợp
    public Page<Product> searchAndFilterProducts(String keyword, Long categoryId, String size,
            Double minPrice, Double maxPrice, Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            java.util.List<Predicate> predicates = new java.util.ArrayList<>();

            // Tìm kiếm theo tên
            if (keyword != null && !keyword.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("productName")), "%" + keyword.toLowerCase() + "%"));
            }

            // Áp dụng các bộ lọc
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }

            // Lọc theo size (nếu có field size trong entity)
            if (size != null && !size.trim().isEmpty()) {
                // predicates.add(cb.equal(root.get("size"), size));
                // Tạm thời bỏ qua vì entity Product chưa có field size
            }

            // Lọc theo giá (nếu có field price trong entity)
            if (minPrice != null) {
                // predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
                // Tạm thời bỏ qua vì entity Product chưa có field price
            }

            if (maxPrice != null) {
                // predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
                // Tạm thời bỏ qua vì entity Product chưa có field price
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return productRepository.findAll(spec, pageable);
    }

    // Lấy sản phẩm theo ID
    public Product getProductById(Long productId) {
        return productRepository.findByProductId(productId);
    }

    public List<Category> getAllCategoryIds() {
        return productRepository.findAllCategories();
    }

    public List<Product> getAllProductsWithVariants() {
        return productRepository.findAllWithVariants();
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    public Product addProduct(ProductAddRequest request) {
        // Create Product
        Product product = new Product();
        product.setProductName(request.getProductName());
        product.setDescription(request.getDescription());
        product.setAccountId(request.getAccountId());
        
        // Set Category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        product.setCategory(category);
        
        // Save Product first
        Product savedProduct = productRepository.save(product);
        
        // Create ProductVariants if provided
        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            List<ProductVariant> variants = new ArrayList<>();
            
            for (ProductVariantRequest variantRequest : request.getVariants()) {
                ProductVariant variant = new ProductVariant();
                variant.setProduct(savedProduct);
                
                // Set Color
                Color color = colorRepository.findById(variantRequest.getColorId())
                        .orElseThrow(() -> new RuntimeException("Color not found"));
                variant.setColor(color);
                
                // Set CategorySize
                CategorySize categorySize = categorySizeRepository.findById(variantRequest.getCategorySizeId())
                        .orElseThrow(() -> new RuntimeException("Size not found"));
                variant.setCategorySize(categorySize);
                
                variant.setPrice(variantRequest.getPrice());
                variant.setStock(variantRequest.getStock());
                
                // Handle image upload
                if (variantRequest.getImageFile() != null && !variantRequest.getImageFile().isEmpty()) {
                    try {
                        String imageUrl = cloudinaryService.uploadImage(variantRequest.getImageFile());
                        variant.setImageUrl(imageUrl);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to upload image to Cloudinary: " + e.getMessage());
                    }
                }
                
                variants.add(variant);
            }
            
            productVariantRepository.saveAll(variants);
        }
        
        return savedProduct;
    }
}
