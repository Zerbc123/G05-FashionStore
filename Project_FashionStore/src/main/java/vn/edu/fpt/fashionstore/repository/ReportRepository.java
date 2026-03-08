package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.fashionstore.entity.ProductVariant;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<ProductVariant, Long> {

    @Query("SELECT p.productName, v.stock, c.categoryName, " +
           "CASE WHEN SUM(oi.quantity) IS NULL THEN 0 ELSE SUM(oi.quantity) END as soldQuantity " +
           "FROM ProductVariant v " +
           "JOIN v.product p " +
           "JOIN p.category c " +
           "LEFT JOIN OrderItem oi ON v.variantId = oi.variant.variantId " +
           "LEFT JOIN Orders o ON oi.order.orderId = o.orderId AND o.status IN ('Completed', 'Pending') " +
           "GROUP BY p.productName, v.variantId, v.stock, c.categoryName " +
           "ORDER BY v.stock ASC")
    List<Object[]> getInventoryReport();

    @Query("SELECT p.productName, v.stock, c.categoryName, " +
           "CASE WHEN SUM(oi.quantity) IS NULL THEN 0 ELSE SUM(oi.quantity) END as soldQuantity " +
           "FROM ProductVariant v " +
           "JOIN v.product p " +
           "JOIN p.category c " +
           "LEFT JOIN OrderItem oi ON v.variantId = oi.variant.variantId " +
           "LEFT JOIN Orders o ON oi.order.orderId = o.orderId AND o.status IN ('Completed', 'Pending') " +
           "WHERE v.stock < :threshold " +
           "GROUP BY p.productName, v.variantId, v.stock, c.categoryName " +
           "ORDER BY v.stock ASC")
    List<Object[]> getLowStockProducts(@Param("threshold") Integer threshold);

    @Query("SELECT c.categoryName, COUNT(p.productId) as productCount, " +
           "SUM(v.stock) as totalStock, " +
           "CASE WHEN SUM(oi.quantity) IS NULL THEN 0 ELSE SUM(oi.quantity) END as soldQuantity " +
           "FROM ProductVariant v " +
           "JOIN v.product p " +
           "JOIN p.category c " +
           "LEFT JOIN OrderItem oi ON v.variantId = oi.variant.variantId " +
           "LEFT JOIN Orders o ON oi.order.orderId = o.orderId AND o.status IN ('Completed', 'Pending') " +
           "GROUP BY c.categoryId, c.categoryName " +
           "ORDER BY productCount DESC")
    List<Object[]> getInventoryByCategory();
}
