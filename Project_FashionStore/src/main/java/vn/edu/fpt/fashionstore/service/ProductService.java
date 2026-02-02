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
                criteriaBuilder.lower(root.get("productName")),
                "%" + keyword.toLowerCase() + "%"
            );
        };
        return productRepository.findAll(spec, pageable);
    }

    // Lọc sản phẩm theo nhiều tiêu chí
    public Page<Product> filterProducts(Long categoryId, Long accountId, Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            java.util.List<Predicate> predicates = new java.util.ArrayList<>();
            
            // Lọc theo danh mục
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            
            // Lọc theo tài khoản
            if (accountId != null) {
                predicates.add(cb.equal(root.get("accountId"), accountId));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return productRepository.findAll(spec, pageable);
    }

    // Tìm kiếm và lọc kết hợp
    public Page<Product> searchAndFilterProducts(String keyword, Long categoryId, 
                                               Long accountId, Pageable pageable) {
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
            
            if (accountId != null) {
                predicates.add(cb.equal(root.get("accountId"), accountId));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return productRepository.findAll(spec, pageable);
    }

    // Lấy sản phẩm theo ID
    public Product getProductById(Long productId) {
        return productRepository.findByProductId(productId);
    }

    // Lấy tất cả category IDs
    public List<Long> getAllCategoryIds() {
        return productRepository.findAllCategoryIds();
    }
}
