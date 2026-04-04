package vn.edu.fpt.fashionstore.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.Predicate;
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
            if (query.getResultType() == Long.class) {
                query.distinct(true);
            } else {
                query.groupBy(root.get("productId"), root.get("productName"), root.get("description"),
                        root.get("category"), root.get("accountId"));
            }

            // Xử lý sắp xếp theo giá cho SQL Server khi dùng GROUP BY
            Sort sort = pageable.getSort();
            if (sort != null && sort.isSorted()) {
                sort.forEach(order -> {
                    if (order.getProperty().equals("variants.price")) {
                        Join<Product, ProductVariant> variants = root.join("variants", JoinType.LEFT);
                        if (order.getDirection().isAscending()) {
                            query.orderBy(cb.asc(cb.min(variants.get("price"))));
                        } else {
                            query.orderBy(cb.desc(cb.min(variants.get("price"))));
                        }
                    }
                });
            }

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNotEmpty(root.get("variants")));
            return cb.and(predicates.toArray(new Predicate[0]));
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

    // Tối ưu hàm Search và Filter để lấy thông tin từ bảng Variant (Price, Size,
    // Color)
    @Transactional(readOnly = true)
    public Page<Product> searchAndFilterProducts(String keyword, Integer categoryId, String categoryName, String size,
            String color, String stockStatus,
            Double minPrice, Double maxPrice, Pageable pageable) {

        Specification<Product> spec = (root, query, cb) -> {
            // Use DISTINCT only for count queries, GROUP BY for result queries to handle
            // SQL Server sorting
            if (query.getResultType() == Long.class) {
                query.distinct(true);
            } else {
                query.groupBy(root.get("productId"), root.get("productName"), root.get("description"),
                        root.get("category"), root.get("accountId"));
            }
            List<Predicate> predicates = new ArrayList<>();

            // 1. Tìm theo tên sản phẩm
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("productName")), "%" + keyword.toLowerCase() + "%"));
            }

            // 2. Lọc theo ID danh mục
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("categoryId"), categoryId));
            }

            // 3. Lọc theo tên danh mục
            if (categoryName != null && !categoryName.isBlank()) {
                if (categoryName.equalsIgnoreCase("Phụ kiện")) {
                    List<String> accessoryCategories = List.of(
                            "túi xách", "giày dép", "mũ nón", "đồng hồ", "mắt kính", "phụ kiện khác", "phụ kiện");
                    predicates.add(cb.lower(root.get("category").get("categoryName")).in(accessoryCategories));
                } else {
                    predicates.add(
                            cb.equal(cb.lower(root.get("category").get("categoryName")), categoryName.toLowerCase()));
                }
            }

            // 4. Lọc biến thể bằng Subquery để tránh nhân bản dữ liệu (DISTINCT)
            boolean hasOtherVariantFilter = (size != null && !size.isBlank()) ||
                    (color != null && !color.isBlank()) ||
                    (minPrice != null && minPrice > 0) ||
                    (maxPrice != null && maxPrice > 0);

            boolean hasStockStatusFilter = (stockStatus != null && !stockStatus.isBlank()
                    && !stockStatus.equals("all"));

            if (hasOtherVariantFilter) {
                jakarta.persistence.criteria.Subquery<Integer> subquery = query.subquery(Integer.class);
                Root<ProductVariant> subRoot = subquery.from(ProductVariant.class);
                subquery.select(cb.literal(1));

                List<Predicate> subPredicates = new ArrayList<>();
                subPredicates.add(cb.equal(subRoot.get("product"), root));

                if (size != null && !size.isBlank()) {
                    subPredicates.add(cb.equal(subRoot.get("categorySize").get("sizeName"), size));
                }
                if (color != null && !color.isBlank()) {
                    subPredicates.add(cb.equal(subRoot.get("color").get("colorName"), color));
                }
                if (minPrice != null && minPrice > 0) {
                    subPredicates.add(cb.greaterThanOrEqualTo(subRoot.get("price"), minPrice));
                }
                if (maxPrice != null && maxPrice > 0) {
                    subPredicates.add(cb.lessThanOrEqualTo(subRoot.get("price"), maxPrice));
                }
                subquery.where(cb.and(subPredicates.toArray(new Predicate[0])));
                predicates.add(cb.exists(subquery));
            }

            if (hasStockStatusFilter) {
                Subquery<Long> sumSubquery = query.subquery(Long.class);
                Root<ProductVariant> sumRoot = sumSubquery.from(ProductVariant.class);
                sumSubquery.select(cb.sum(sumRoot.get("stock")));
                sumSubquery.where(cb.equal(sumRoot.get("product"), root));

                if (stockStatus.equalsIgnoreCase("in-stock")) {
                    predicates.add(cb.greaterThan(sumSubquery, 20L));
                } else if (stockStatus.equalsIgnoreCase("low-stock")) {
                    predicates.add(cb.and(cb.greaterThan(sumSubquery, 0L), cb.lessThanOrEqualTo(sumSubquery, 20L)));
                } else if (stockStatus.equalsIgnoreCase("out-stock")) {
                    predicates.add(cb.or(cb.isNull(sumSubquery), cb.equal(sumSubquery, 0L)));
                }
            }

            // 5. Sắp xếp theo giá (cần Join nhưng chỉ cho orderBy)
            if (pageable.getSort() != null
                    && pageable.getSort().stream().anyMatch(o -> o.getProperty().equals("variants.price"))) {
                Join<Product, ProductVariant> sortJoin = root.join("variants", JoinType.LEFT);
                pageable.getSort().forEach(order -> {
                    if (order.getProperty().equals("variants.price")) {
                        if (order.getDirection().isAscending()) {
                            query.orderBy(cb.asc(cb.min(sortJoin.get("price"))));
                        } else {
                            query.orderBy(cb.desc(cb.min(sortJoin.get("price"))));
                        }
                    }
                });
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable p = pageable;
        if (pageable.getSort() != null
                && pageable.getSort().stream().anyMatch(o -> o.getProperty().equals("variants.price"))) {
            p = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted());
        }

        Page<Product> result = productRepository.findAll(spec, p);

        // 6. Force load và lọc biến thể để hiển thị giá khớp filter
        boolean hasVarFilter = (size != null && !size.isBlank()) ||
                (color != null && !color.isBlank()) ||
                (minPrice != null && minPrice > 0) ||
                (maxPrice != null && maxPrice > 0);

        result.forEach(product -> {
            if (product.getVariants() != null) {
                List<ProductVariant> matchedVariants = product.getVariants().stream()
                        .filter(v -> {
                            boolean match = true;
                            if (size != null && !size.isBlank())
                                match &= v.getCategorySize() != null && size.equals(v.getCategorySize().getSizeName());
                            if (color != null && !color.isBlank())
                                match &= v.getColor() != null && color.equals(v.getColor().getColorName());
                            if (minPrice != null && minPrice > 0)
                                match &= v.getPrice() != null && v.getPrice() >= minPrice;
                            if (maxPrice != null && maxPrice > 0)
                                match &= v.getPrice() != null && v.getPrice() <= maxPrice;
                            return match;
                        })
                        .sorted((v1, v2) -> v1.getPrice().compareTo(v2.getPrice()))
                        .collect(java.util.stream.Collectors.toList());

                if (hasVarFilter && !matchedVariants.isEmpty()) {
                    product.setVariants(matchedVariants);
                } else {
                    product.getVariants().sort((v1, v2) -> v1.getPrice().compareTo(v2.getPrice()));
                }
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

            // Always ensure the product has variants for public view
            predicates.add(cb.isNotEmpty(root.get("variants")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        var combinedResult = productRepository.findAll(combinedSpec, pageable);
        return combinedResult;
    }

    @Transactional(readOnly = true)
    public Page<Product> filterByCategoryName(String categoryName, Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> {
            if (query.getResultType() == Long.class) {
                query.distinct(true);
            } else {
                query.groupBy(root.get("productId"), root.get("productName"), root.get("description"),
                        root.get("category"), root.get("accountId"));
            }

            if (pageable.getSort() != null && pageable.getSort().isSorted()) {
                pageable.getSort().forEach(order -> {
                    if (order.getProperty().equals("variants.price")) {
                        Join<Product, ProductVariant> variants = root.join("variants", JoinType.LEFT);
                        if (order.getDirection().isAscending()) {
                            query.orderBy(cb.asc(cb.min(variants.get("price"))));
                        } else {
                            query.orderBy(cb.desc(cb.min(variants.get("price"))));
                        }
                    }
                });
            }

            List<Predicate> predicates = new ArrayList<>();
            
            if (categoryName != null && !categoryName.isBlank()) {
                if (categoryName.equalsIgnoreCase("Phụ kiện")) {
                    List<String> accessoryCategories = List.of(
                            "túi xách", "giày dép", "mũ nón", "đồng hồ", "mắt kính", "phụ kiện khác", "phụ kiện");
                    predicates.add(cb.lower(root.get("category").get("categoryName")).in(accessoryCategories));
                } else {
                    predicates.add(cb.equal(cb.lower(root.get("category").get("categoryName")),
                        categoryName.toLowerCase()));
                }
            }
            
            // Always ensure the product has variants
            predicates.add(cb.isNotEmpty(root.get("variants")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable p = pageable;
        if (pageable.getSort() != null
                && pageable.getSort().stream().anyMatch(o -> o.getProperty().equals("variants.price"))) {
            p = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted());
        }

        Page<Product> result = productRepository.findAll(spec, p);

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

    public List<ProductRepository.ProductHomeInfo> getHomeProducts() {
        return productRepository.getAllProductHome();
    }

    @Transactional
    public String deleteProduct(Long productId) {
        Product product = productRepository.findByProductId(productId);
        if (product == null) {
            return "Không tìm thấy sản phẩm";
        }
        
        // Check if any variants are linked to orders
        List<ProductVariant> variants = productVariantRepository.findByProduct_ProductId(productId);
        for (ProductVariant variant : variants) {
            if (orderItemRepository.existsByProductVariantVariantId(variant.getVariantId())) {
                return "Không thể xóa sản phẩm: Một số biến thể đã được sử dụng trong đơn hàng";
            }
        }
        
        // Check if product is in any wishlist (Optional)
        // wishlistRepository.deleteByProduct_ProductId(productId);
        
        productVariantRepository.deleteByProduct_ProductId(productId);
        productRepository.delete(product);
        return "SUCCESS";
    }

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
        if (productName.matches(".*\\d.*")) {
            return "Product name cannot contain numbers";
        }
        if (description != null && description.length() > 5000) {
            return "Description must not exceed 5000 characters";
        }
        if (categoryId == null) {
            return "Category is required";
        }
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
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return "Only image files are allowed";
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            return "Image size must be less than 5MB";
        }
        return null; // No validation errors
    }

    private boolean hasVariantFilter(String size, String color, Double minPrice, Double maxPrice) {
        return (size != null && !size.isBlank()) ||
                (color != null && !color.isBlank()) ||
                (minPrice != null && minPrice > 0) ||
                (maxPrice != null && maxPrice > 0);
    }
}
