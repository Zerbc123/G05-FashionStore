package vn.edu.fpt.fashionstore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.edu.fpt.fashionstore.entity.Product;
import vn.edu.fpt.fashionstore.repository.ProductRepository;

import jakarta.persistence.criteria.Predicate;
import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

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
                criteriaBuilder.lower(root.get("name")),
                "%" + keyword.toLowerCase() + "%"
            );
        };
        return productRepository.findAll(spec, pageable);
    }

    // Lọc sản phẩm theo nhiều tiêu chí
    public Page<Product> filterProducts(String category, Double minPrice, Double maxPrice, 
                                     String size, Boolean isNew, Boolean isSale, Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            java.util.List<Predicate> predicates = new java.util.ArrayList<>();
            
            // Lọc theo danh mục
            if (category != null && !category.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.toLowerCase()));
            }
            
            // Lọc theo khoảng giá
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            
            // Lọc theo kích thước
            if (size != null && !size.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("size")), size.toLowerCase()));
            }
            
            // Lọc theo sản phẩm mới
            if (isNew != null) {
                predicates.add(cb.equal(root.get("isNew"), isNew));
            }
            
            // Lọc theo sản phẩm giảm giá
            if (isSale != null) {
                predicates.add(cb.equal(root.get("isSale"), isSale));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return productRepository.findAll(spec, pageable);
    }

    // Tìm kiếm và lọc kết hợp
    public Page<Product> searchAndFilterProducts(String keyword, String category, 
                                               Double minPrice, Double maxPrice, 
                                               String size, Boolean isNew, Boolean isSale, 
                                               Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            java.util.List<Predicate> predicates = new java.util.ArrayList<>();
            
            // Tìm kiếm theo tên
            if (keyword != null && !keyword.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"));
            }
            
            // Áp dụng các bộ lọc
            if (category != null && !category.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.toLowerCase()));
            }
            
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            
            if (size != null && !size.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("size")), size.toLowerCase()));
            }
            
            if (isNew != null) {
                predicates.add(cb.equal(root.get("isNew"), isNew));
            }
            
            if (isSale != null) {
                predicates.add(cb.equal(root.get("isSale"), isSale));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return productRepository.findAll(spec, pageable);
    }

    // Lấy sản phẩm theo ID
    public Product getProductById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    // Lấy tất cả danh mục
    public List<String> getAllCategories() {
        return productRepository.findAllCategories();
    }

    // Lấy tất cả kích thước
    public List<String> getAllSizes() {
        return productRepository.findAllSizes();
    }
}
