package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.fashionstore.entity.Category;
import vn.edu.fpt.fashionstore.entity.Product;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /* =========================================================
     * PHẦN 1: CÁC HÀM CƠ BẢN
     * ========================================================= */

    // LẤY CATEGORY ID TỪ PRODUCT
    @Query("SELECT DISTINCT p.category FROM Product p")
    List<Category> findAllCategories();

    Product findByProductId(Long productId);

    // Hiện sản phẩm bán chạy/mới nhất (Trả về Entity gốc)
    List<Product> findTop4ByOrderByProductIdDesc();

    // Lấy danh sách sản phẩm kèm Variants và Category để hiện ở trang chủ/danh sách
    @Query("SELECT DISTINCT p FROM Product p " +
            "LEFT JOIN FETCH p.category " +
            "LEFT JOIN FETCH p.variants")
    List<Product> findAllWithVariants();

    // QUAN TRỌNG: Lấy chi tiết 1 sản phẩm kèm toàn bộ thông tin Color/Size
    // Dùng cái này cho trang Product Detail để performance tốt nhất
    @Query("SELECT p FROM Product p " +
            "LEFT JOIN FETCH p.variants v " +
            "LEFT JOIN FETCH v.color " +
            "LEFT JOIN FETCH v.categorySize " +
            "LEFT JOIN FETCH p.category " +
            "WHERE p.productId = :productId")
    Product findByProductIdWithVariants(@Param("productId") Long productId);

    /* =========================================================
     * PHẦN 2: TỐI ƯU CHO TRANG CHỦ (Projection)
     * Dùng cái này để hiển thị list sản phẩm có Ảnh + Giá Min
     * ========================================================= */

    // 1. Định nghĩa Interface "Hứng" dữ liệu (Nằm ngay trong file này)
    public interface ProductHomeInfo {
        Long getId();             // Hứng alias 'id'

        String getName();         // Hứng alias 'name'

        Double getPrice();        // Hứng alias 'price' (Giá thấp nhất)

        String getImage();        // Hứng alias 'image' (Ảnh đại diện)

        String getCategoryName(); // Hứng alias 'categoryName'
    }

    // 2. Câu truy vấn tối ưu lấy dữ liệu vào Interface trên
    @Query("SELECT " +
            "p.productId as id, " +
            "p.productName as name, " +
            "c.categoryName as categoryName, " +
            "MIN(v.price) as price, " +          // Lấy giá thấp nhất trong các biến thể
            "MIN(v.imageUrl) as image " +       // Lấy 1 ảnh đại diện (chú ý tên trường trong Entity Variant là imageUrl hay image_url)
            "FROM Product p " +
            "LEFT JOIN p.variants v " +          // Kết nối bảng biến thể
            "LEFT JOIN p.category c " +          // Kết nối bảng danh mục
            "GROUP BY p.productId, p.productName, c.categoryName")
    List<ProductHomeInfo> getAllProductHome();

    // ĐÃ SỬA LẠI: Trả về ProductHomeInfo thay vì Product để giao diện HTML đọc được
    @Query("SELECT " +
            "p.productId as id, " +
            "p.productName as name, " +
            "c.categoryName as categoryName, " +
            "MIN(v.price) as price, " +
            "MIN(v.imageUrl) as image " +
            "FROM OrderItem oi " +
            "JOIN oi.productVariant v " +
            "JOIN v.product p " +
            "LEFT JOIN p.category c " +
            "WHERE oi.order.status = 'COMPLETED' " +
            "GROUP BY p.productId, p.productName, c.categoryName " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<ProductHomeInfo> findTopSellingProducts(Pageable pageable);
}
