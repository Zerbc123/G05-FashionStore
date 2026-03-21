package vn.edu.fpt.fashionstore.service;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.fashionstore.entity.*;
import vn.edu.fpt.fashionstore.repository.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import jakarta.persistence.criteria.Predicate;
import jakarta.validation.constraints.NotNull;

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
    
    @Autowired
    private OrderItemRepository orderItemRepository;

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

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public Product updateProduct(Long productId, Product productDetails) {
        Product existingProduct = productRepository.findByProductId(productId);
        if (existingProduct != null) {
            existingProduct.setProductName(productDetails.getProductName());
            existingProduct.setDescription(productDetails.getDescription());
            existingProduct.setCategory(productDetails.getCategory());
            return productRepository.save(existingProduct);
        }
        return null;
    }

    public Product getProductWithVariantsById(Long productId) {
        Product product = productRepository.findByProductId(productId);
        if (product != null && product.getVariants() == null) {
            product.setVariants(productVariantRepository.findByProduct_ProductId(productId));
        }
        return product;
    }

    // Hiện sản phẩm bán chạy
    public List<ProductRepository.ProductHomeInfo> getHomeProducts() {
        // Gọi thẳng hàm tối ưu trong Repository, không cần xử lý thủ công nữa
        return productRepository.getAllProductHome();
    }
    
    // Delete product with validation
    @Transactional
    public boolean deleteProduct(Long productId) {
        Product product = productRepository.findByProductId(productId);
        if (product == null) {
            return false;
        }
        
        // // Check if product has any order items
        // boolean hasOrderItems = orderItemRepository.existsByProductVariantProductProductId(productId);
        // if (hasOrderItems) {
        //     throw new RuntimeException("Không thể xóa sản phẩm này vì có đơn hàng liên quan");
        // }
        
        // // Check if all variants are out of stock
        // long inStockVariantsCount = productVariantRepository.countInStockVariantsByProductId(productId);
        // if (inStockVariantsCount > 0) {
        //     throw new RuntimeException("Không thể xóa sản phẩm này vì vẫn còn biến thể trong kho");
        // }
        
        // Delete all variants first (due to foreign key constraint)
        productVariantRepository.deleteByProduct_ProductId(productId);
        
        // Delete the product
        productRepository.delete(product);
        
        return true;
    }
    
    // Validation methods for product creation
    public String validateProductData(String productName, String description, Integer categoryId) {
        if (productName == null || productName.trim().isEmpty()) {
            return "Product name is required";
        }
        
        if (productName.trim().length() < 3) {
            return "Product name must be at least 3 characters long";
        }
        
        if (productName.trim().length() > 255) {
            return "Product name must not exceed 255 characters";
        }
        
        // Check if product name contains numbers
        if (productName.matches(".*\\d.*")) {
            return "Product name cannot contain numbers";
        }
        
        if (description != null && description.length() > 5000) {
            return "Description must not exceed 5000 characters";
        }
        
        if (categoryId == null) {
            return "Category is required";
        }
        
        // Check if category exists
        if (!categoryRepository.existsById(categoryId)) {
            return "Selected category not found";
        }
        
        return null; // No validation errors
    }
    
    public String validateVariantData(Integer colorId, Integer sizeId, Double price, Integer stock) {
        if (colorId == null) {
            return "Color is required";
        }
        
        if (!colorRepository.existsById(colorId)) {
            return "Selected color not found";
        }
        
        if (sizeId == null) {
            return "Size is required";
        }
        
        if (!categorySizeRepository.existsById(sizeId)) {
            return "Selected size not found";
        }
        
        if (price == null) {
            return "Price is required";
        }
        
        if (price < 0) {
            return "Price must be greater than or equal to 0";
        }
        
        if (stock == null) {
            return "Stock is required";
        }
        
        if (stock < 0) {
            return "Stock must be greater than or equal to 0";
        }
        
        return null; // No validation errors
    }
    
    public String validateImageFile(org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "Image file is required";
        }
        
        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return "Only image files are allowed";
        }
        
        // Validate file size (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            return "Image size must be less than 5MB";
        }
        
        return null; // No validation errors
    }
}
