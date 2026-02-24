package vn.edu.fpt.fashionstore.service;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.fashionstore.entity.Category;
import vn.edu.fpt.fashionstore.entity.Product;
import vn.edu.fpt.fashionstore.entity.ProductVariant;
import vn.edu.fpt.fashionstore.repository.ProductRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // Lấy tất cả sản phẩm với phân trang
    @Transactional(readOnly = true)
    public Page<Product> getAllProducts(Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            query.distinct(true);
            return cb.conjunction();
        };
        
        // Lấy kết quả trước, sau đó fetch variants và category
        Page<Product> result = productRepository.findAll(spec, pageable);
        
        // Force load variants và category sau khi query
        result.forEach(product -> {
            if (product.getVariants() != null) {
                product.getVariants().size(); // Force load
            }
            if (product.getCategory() != null) {
                product.getCategory().getCategoryName(); // Force load
            }
        });
        
        return result;
    }
    
    // Lấy tất cả sản phẩm với variants (cho hiển thị)
    public List<Product> getAllProductsWithVariants() {
        return productRepository.findAllWithVariants();
    }

    // Tìm kiếm sản phẩm theo tên
    public Page<Product> searchProducts(String keyword, Pageable pageable) {
        Specification<Product> spec = (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("productName")),
                "%" + keyword.toLowerCase() + "%"
            );
        };
        return productRepository.findAll(spec, pageable);
    }

    // Lọc sản phẩm theo nhiều tiêu chí
    public Page<Product> filterProducts(Long categoryId, String size, Double minPrice, Double maxPrice, Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            java.util.List<Predicate> predicates = new java.util.ArrayList<>();

            // Lọc theo danh mục
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("cateId"), categoryId));
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

    // Tối ưu hàm Search và Filter để lấy thông tin từ bảng Variant (Price, Size, Color)
    public Page<Product> searchAndFilterProducts(String keyword, Long categoryId, String size, String color,
                                                 Double minPrice, Double maxPrice, Pageable pageable) {
        
        Specification<Product> spec = (root, query, cb) -> {
            query.distinct(true); // Tránh trùng lặp sản phẩm khi join nhiều variant
            List<Predicate> predicates = new ArrayList<>();

            // 1. Tìm theo tên sản phẩm
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("productName")), "%" + keyword.toLowerCase() + "%"));
            }

            // 2. Lọc theo danh mục (category_id)
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("categoryId"), categoryId));
            }

            // 3. Lọc theo variants - chỉ khi có variant filter
            if (size != null && !size.isBlank() || color != null && !color.isBlank() || minPrice != null || maxPrice != null) {
                // Dùng LEFT JOIN và thêm điều kiện vào WHERE clause
                Join<Product, ProductVariant> variants = root.join("variants", JoinType.LEFT);
                
                if (size != null && !size.isBlank()) {
                    predicates.add(cb.equal(variants.get("categorySize").get("sizeName"), size));
                }
                if (color != null && !color.isBlank()) {
                    predicates.add(cb.equal(variants.get("color").get("colorName"), color));
                }
                if (minPrice != null) {
                    predicates.add(cb.greaterThanOrEqualTo(variants.get("price"), minPrice));
                }
                if (maxPrice != null) {
                    predicates.add(cb.lessThanOrEqualTo(variants.get("price"), maxPrice));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        // Lấy kết quả trước, sau đó fetch variants và category
        Page<Product> result = productRepository.findAll(spec, pageable);
        
        // Force load variants và category sau khi query
        result.forEach(product -> {
            if (product.getVariants() != null) {
                product.getVariants().size(); // Force load
            }
            if (product.getCategory() != null) {
                product.getCategory().getCategoryName(); // Force load
            }
        });
        
        return result;
    }
    
    // Method test cực đơn giản để debug từng bước
    public Page<Product> debugFilter(Long categoryId, String color, String size, Pageable pageable) {
        System.out.println("=== DEBUG FILTER STEP BY STEP ===");
        System.out.println("categoryId: " + categoryId);
        System.out.println("color: " + color);
        System.out.println("size: " + size);
        
        // Bước 1: Test chỉ category filter
        if (categoryId != null) {
            Specification<Product> catSpec = (root, query, cb) -> {
                if (query.getResultType() != Long.class) {
                    root.fetch("category", JoinType.LEFT);
                }
                query.distinct(true);
                return cb.equal(root.get("category").get("categoryId"), categoryId);
            };
            var catResult = productRepository.findAll(catSpec, pageable);
            System.out.println("Category only result: " + catResult.getTotalElements() + " products");
        }
        
        // Bước 2: Test chỉ color filter
        if (color != null && !color.isBlank()) {
            Specification<Product> colorSpec = (root, query, cb) -> {
                if (query.getResultType() != Long.class) {
                    root.fetch("variants", JoinType.LEFT);
                    root.fetch("variants").fetch("color", JoinType.LEFT);
                }
                query.distinct(true);
                Join<Product, ProductVariant> variants = root.join("variants", JoinType.LEFT);
                return cb.equal(variants.get("color").get("colorName"), color);
            };
            var colorResult = productRepository.findAll(colorSpec, pageable);
            System.out.println("Color only result: " + colorResult.getTotalElements() + " products");
        }
        
        // Bước 3: Test chỉ size filter
        if (size != null && !size.isBlank()) {
            Specification<Product> sizeSpec = (root, query, cb) -> {
                if (query.getResultType() != Long.class) {
                    root.fetch("variants", JoinType.LEFT);
                    root.fetch("variants").fetch("categorySize", JoinType.LEFT);
                }
                query.distinct(true);
                Join<Product, ProductVariant> variants = root.join("variants", JoinType.LEFT);
                return cb.equal(variants.get("categorySize").get("sizeName"), size);
            };
            var sizeResult = productRepository.findAll(sizeSpec, pageable);
            System.out.println("Size only result: " + sizeResult.getTotalElements() + " products");
        }
        
        // Bước 4: Test kết hợp
        Specification<Product> combinedSpec = (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();
            
            if (query.getResultType() != Long.class) {
                root.fetch("variants", JoinType.LEFT);
                root.fetch("category", JoinType.LEFT);
            }
            
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("categoryId"), categoryId));
            }
            
            if (color != null && !color.isBlank()) {
                Join<Product, ProductVariant> variants = root.join("variants", JoinType.LEFT);
                predicates.add(cb.equal(variants.get("color").get("colorName"), color));
            }
            
            if (size != null && !size.isBlank()) {
                Join<Product, ProductVariant> variants = root.join("variants", JoinType.LEFT);
                predicates.add(cb.equal(variants.get("categorySize").get("sizeName"), size));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        var combinedResult = productRepository.findAll(combinedSpec, pageable);
        System.out.println("Combined result: " + combinedResult.getTotalElements() + " products");
        System.out.println("=== END DEBUG ===");
        
        return combinedResult;
    }
    
    // Helper method để kiểm tra có filter variant không
    private boolean hasVariantFilter(String size, String color, Double minPrice, Double maxPrice) {
        return (size != null && !size.isBlank()) || 
               (color != null && !color.isBlank()) || 
               minPrice != null || 
               maxPrice != null;
    }

    // Sửa lỗi trong ảnh bạn gửi
    public Product getProductById(Long productId) {
        // Đảm bảo tên phương thức này giống hệt tên trong Repository
        return productRepository.findByProductIdWithVariants(productId);
    }

    public List<Category> findAllCategories() {
        return productRepository.findAllCategories();
    }

    // Hiện sản phẩm bán chạy
    public List<ProductRepository.ProductHomeInfo> getHomeProducts() {
        // Gọi thẳng hàm tối ưu trong Repository, không cần xử lý thủ công nữa
        return productRepository.getAllProductHome();
    }
}
