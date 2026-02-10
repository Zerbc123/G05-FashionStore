package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.fashionstore.entity.Category;
import vn.edu.fpt.fashionstore.entity.Product;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /* =========================================================
     * PHẦN 1: CÁC HÀM CŨ CỦA BẠN (GIỮ NGUYÊN ĐỂ KHÔNG LỖI)
     * ========================================================= */

    // LẤY CATEGORY ID TỪ PRODUCT
    @Query("SELECT DISTINCT p.category FROM Product p")
    List<Category> findAllCategories();

    Product findByProductId(Long productId);

    // Hiện sản phẩm bán chạy/mới nhất (Trả về Entity gốc)
    List<Product> findTop4ByOrderByProductIdDesc();


//    /* =========================================================
//     * PHẦN 2: PHẦN MỚI - TỐI ƯU CHO TRANG CHỦ (Projection)
//     * Dùng cái này để hiển thị list sản phẩm có Ảnh + Giá Min
//     * ========================================================= */
//
//    // 1. Định nghĩa Interface "Hứng" dữ liệu (Nằm ngay trong file này)
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
}