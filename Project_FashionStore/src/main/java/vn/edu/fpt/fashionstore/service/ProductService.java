package vn.edu.fpt.fashionstore.service;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.edu.fpt.fashionstore.entity.Category;
import vn.edu.fpt.fashionstore.entity.Product;
import vn.edu.fpt.fashionstore.entity.ProductVariant;
import vn.edu.fpt.fashionstore.repository.ProductRepository;

import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    // Lấy tất cả sản phẩm với phân trang
    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
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

    // Tối ưu hàm Search và Filter để lấy thông tin từ bảng Variant (Price, Size)
    public Page<Product> searchAndFilterProducts(String keyword, Long categoryId, String size,
                                                 Double minPrice, Double maxPrice, Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            query.distinct(true); // Tránh trùng lặp sản phẩm khi join nhiều variant
            List<Predicate> predicates = new ArrayList<>();

            // Thực hiện JOIN giữa Product và ProductVariant
            // Dựa trên quan hệ OneToMany trong Entity [cite: 17]
            Join<Product, ProductVariant> variants = root.join("variants", JoinType.LEFT);

            // 1. Tìm theo tên sản phẩm
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("productName")), "%" + keyword.toLowerCase() + "%"));
            }

            // 2. Lọc theo danh mục (category_id) [cite: 10]
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("categoryId"), categoryId));
            }

            // 3. Lọc theo Size (phải join qua bảng Category_Size) [cite: 6, 17]
            if (size != null && !size.isBlank()) {
                predicates.add(cb.equal(variants.get("categorySize").get("sizeName"), size));
            }

            // 4. Lọc theo khoảng giá (nằm ở bảng ProductVariant) [cite: 11]
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(variants.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(variants.get("price"), maxPrice));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return productRepository.findAll(spec, pageable);
    }

    // Sửa lỗi trong ảnh bạn gửi
    public Product getProductById(Long productId) {
        // Đảm bảo tên phương thức này giống hệt tên trong Repository
        return productRepository.findByProductIdWithVariants(productId);
    }

    public List<Category> getAllCategoryIds() {
        return productRepository.findAllCategories();
    }

    // Hiện sản phẩm bán chạy
    public List<ProductRepository.ProductHomeInfo> getHomeProducts() {
        // Gọi thẳng hàm tối ưu trong Repository, không cần xử lý thủ công nữa
        return productRepository.getAllProductHome();
    }
}
