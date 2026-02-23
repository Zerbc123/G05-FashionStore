package vn.edu.fpt.fashionstore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
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

    // Lọc sản phẩm theo nhiều tiêu chí
    public Page<Product> filterProducts(Long categoryId, String size, String color, String stockStatus, Double minPrice, Double maxPrice, Pageable pageable) {
        Specification<Product> spec = (root, query, criteriaBuilder) -> {
            java.util.List<Predicate> predicates = new java.util.ArrayList<>();

            // Lọc theo danh mục
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("cateId"), categoryId));
            }

            // Lọc theo size (nếu có field size trong entity)
            if (size != null && !size.trim().isEmpty()) {
                // predicates.add(criteriaBuilder.equal(root.get("size"), size));
                // Tạm thời bỏ qua vì entity Product chưa có field size
            }

            // Lọc theo màu sắc
            if (color != null && !color.trim().isEmpty()) {
                // Join với ProductVariant để filter theo color
                Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.LEFT);
                predicates.add(criteriaBuilder.equal(variantJoin.get("color").get("colorName"), color));
            }

            // Lọc theo trạng thái stock
            if (stockStatus != null && !stockStatus.trim().isEmpty()) {
                // Join với ProductVariant để filter theo stock
                Join<Product, ProductVariant> variantJoin = root.join("variants", JoinType.LEFT);
                switch (stockStatus) {
                    case "in-stock":
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(variantJoin.get("stock"), 10));
                        break;
                    case "low-stock":
                        predicates.add(criteriaBuilder.and(
                            criteriaBuilder.greaterThan(variantJoin.get("stock"), 0),
                            criteriaBuilder.lessThan(variantJoin.get("stock"), 10)
                        ));
                        break;
                    case "out-of-stock":
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(variantJoin.get("stock"), 0));
                        break;
                }
            }

            // Lọc theo giá tối thiểu (nếu có field price trong entity)
            if (minPrice != null) {
                // predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
                // Tạm thời bỏ qua vì entity Product chưa có field price
            }

            // Lọc theo giá tối đa (nếu có field price trong entity)
            if (maxPrice != null) {
                // predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
                // Tạm thời bỏ qua vì entity Product chưa có field price
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        return productRepository.findAll(spec, pageable);
    }

    // Tìm kiếm và lọc kết hợp
    public Page<Product> searchAndFilterProducts(String keyword, Long categoryId, String size, String color, String stockStatus, Double minPrice, Double maxPrice, Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            java.util.List<Predicate> predicates = new java.util.ArrayList<>();

            // Tìm kiếm theo tên
            if (keyword != null && !keyword.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("productName")), "%" + keyword.toLowerCase() + "%"));
            }

            // Áp dụng các bộ lọc
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("cateId"), categoryId));
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
    @Transactional(readOnly = true)
    public Product getProductById(Long productId) {
        try {
            Product product = entityManager.find(Product.class, productId);
            if (product != null) {
                // Initialize collections to avoid lazy loading issues
                product.getVariants().size(); // Force initialization
                if (product.getCategory() != null) {
                    product.getCategory().getCategoryName(); // Force initialization
                }
                // Initialize variant relationships
                for (ProductVariant variant : product.getVariants()) {
                    if (variant.getCategorySize() != null) {
                        variant.getCategorySize().getSizeName();
                    }
                    if (variant.getColor() != null) {
                        variant.getColor().getColorName();
                    }
                }
            }
            return product;
        } catch (Exception e) {
            System.err.println("Error loading product: " + e.getMessage());
            e.printStackTrace();
            // Fallback to repository method
            return productRepository.findByProductId(productId);
        }
    }

    public List<Category> getAllCategoryIds() {
        return productRepository.findAllCategories();
    }
}
