package vn.edu.fpt.fashionstore.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    @Transactional(readOnly = true)
    public Page<Product> getAllProducts(Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            query.groupBy(root.get("productId"), root.get("productName"), root.get("description"), root.get("category"), root.get("accountId"));
            
            // Xử lý sắp xếp theo giá cho SQL Server khi dùng GROUP BY
            Sort sort = pageable.getSort();
            if (sort != null && sort.isSorted()) {
                sort.forEach(order -> {
                    if (order.getProperty().equals("variants.price")) {
                        Join<Product, ProductVariant> variants = root.join("variants", JoinType.LEFT);
                        if (order.getDirection().isAscending()) {
                            query.orderBy(cb.asc(cb.min(variants.get("price"))));
                        } else {
                            query.orderBy(cb.desc(cb.max(variants.get("price"))));
                        }
                    }
                });
            }
            
            return cb.conjunction();
        };

        // Nếu đã sắp xếp bằng Specification, gỡ Sort khỏi Pageable để tránh xung đột
        Pageable p = pageable;
        if (pageable.getSort().stream().anyMatch(o -> o.getProperty().equals("variants.price"))) {
            p = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted());
        }
        
        // Lấy kết quả trước, sau đó fetch variants và category
        Page<Product> result = productRepository.findAll(spec, p);
        
        // Force load variants và category sau khi query
        result.forEach(product -> {
            if (product.getVariants() != null) {
                product.getVariants().size(); // Force load
                product.getVariants().sort((v1, v2) -> v1.getPrice().compareTo(v2.getPrice()));
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
                    "%" + keyword.toLowerCase() + "%");
        };
        return productRepository.findAll(spec, pageable);
    }

    // Lọc sản phẩm theo nhiều tiêu chí
    public Page<Product> filterProducts(Integer categoryId, String size, Double minPrice, Double maxPrice,
            Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            java.util.List<Predicate> predicates = new java.util.ArrayList<>();

            // Lọc theo danh mục
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("categoryId"), categoryId));
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
    @Transactional(readOnly = true)
    public Page<Product> searchAndFilterProducts(String keyword, Integer categoryId, String size, String color,
                                                 Double minPrice, Double maxPrice, Pageable pageable) {
        
        Specification<Product> spec = (root, query, cb) -> {
            // Thay DISTINCT bằng GROUP BY để fix lỗi SQL Server
            query.groupBy(root.get("productId"), root.get("productName"), root.get("description"), root.get("category"), root.get("accountId"));
            List<Predicate> predicates = new ArrayList<>();

            // 1. Tìm theo tên sản phẩm
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("productName")), "%" + keyword.toLowerCase() + "%"));
            }

            // 2. Lọc theo danh mục (category_id)
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("categoryId"), categoryId));
            }

            // 3. Lọc theo variants
            Join<Product, ProductVariant> variants = null;
            if (size != null && !size.isBlank() || color != null && !color.isBlank() || minPrice != null || maxPrice != null || 
                (pageable.getSort() != null && pageable.getSort().stream().anyMatch(o -> o.getProperty().equals("variants.price")))) {
                
                variants = root.join("variants", JoinType.LEFT);
                
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

            // 4. Xử lý sắp xếp theo giá cho SQL Server khi dùng GROUP BY
            if (variants != null && pageable.getSort() != null && pageable.getSort().isSorted()) {
                final Join<Product, ProductVariant> finalVariants = variants;
                pageable.getSort().forEach(order -> {
                    if (order.getProperty().equals("variants.price")) {
                        if (order.getDirection().isAscending()) {
                            query.orderBy(cb.asc(cb.min(finalVariants.get("price"))));
                        } else {
                            query.orderBy(cb.desc(cb.max(finalVariants.get("price"))));
                        }
                    }
                });
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // Nếu đã sắp xếp bằng Specification, gỡ Sort khỏi Pageable để tránh xung đột
        Pageable p = pageable;
        if (pageable.getSort() != null && pageable.getSort().stream().anyMatch(o -> o.getProperty().equals("variants.price"))) {
            p = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted());
        }
        
        // Lấy kết quả trước
        Page<Product> result = productRepository.findAll(spec, p);
        
        // Force load variants và category sau khi query
        result.forEach(product -> {
            if (product.getVariants() != null) {
                product.getVariants().size(); // Force load
                product.getVariants().sort((v1, v2) -> v1.getPrice().compareTo(v2.getPrice()));
            }
            if (product.getCategory() != null) {
                product.getCategory().getCategoryName(); // Force load
            }
        });
        
        return result;
    }
    
    // Method test cực đơn giản để debug từng bước
    public Page<Product> debugFilter(Integer categoryId, String color, String size, Pageable pageable) {
        
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
    // Lọc sản phẩm theo tên danh mục
    @Transactional(readOnly = true)
    public Page<Product> filterByCategoryName(String categoryName, Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            query.groupBy(root.get("productId"), root.get("productName"), root.get("description"), root.get("category"), root.get("accountId"));
            
            // Xử lý sắp xếp theo giá cho SQL Server khi dùng GROUP BY
            if (pageable.getSort() != null && pageable.getSort().isSorted()) {
                pageable.getSort().forEach(order -> {
                    if (order.getProperty().equals("variants.price")) {
                        Join<Product, ProductVariant> variants = root.join("variants", JoinType.LEFT);
                        if (order.getDirection().isAscending()) {
                            query.orderBy(cb.asc(cb.min(variants.get("price"))));
                        } else {
                            query.orderBy(cb.desc(cb.max(variants.get("price"))));
                        }
                    }
                });
            }

            if (categoryName != null && !categoryName.isBlank()) {
                return cb.equal(cb.lower(root.get("category").get("categoryName")), 
                              categoryName.toLowerCase());
            }
            return cb.conjunction();
        };

        // Nếu đã sắp xếp bằng Specification, gỡ Sort khỏi Pageable để tránh xung đột
        Pageable p = pageable;
        if (pageable.getSort() != null && pageable.getSort().stream().anyMatch(o -> o.getProperty().equals("variants.price"))) {
            p = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted());
        }
        
        Page<Product> result = productRepository.findAll(spec, p);
        
        // Force load variants và category sau khi query
        result.forEach(product -> {
            if (product.getVariants() != null) {
                product.getVariants().size();
                product.getVariants().sort((v1, v2) -> v1.getPrice().compareTo(v2.getPrice()));
            }
            if (product.getCategory() != null) {
                product.getCategory().getCategoryName();
            }
        });
        
        return result;
    }

    public Product getProductById(Long productId) {
        // Đảm bảo tên phương thức này giống hệt tên trong Repository
        return productRepository.findByProductIdWithVariants(productId);
    }

    public List<Category> findAllCategories() {
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
