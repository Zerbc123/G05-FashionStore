package vn.edu.fpt.fashionstore.repository;

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
    @Query("SELECT DISTINCT p.category FROM Product p")
    List<Category> findAllCategories();

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
}
