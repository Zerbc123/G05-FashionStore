package vn.edu.fpt.fashionstore.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.fashionstore.entity.Product;
import vn.edu.fpt.fashionstore.entity.OrderStatus;

import java.util.List;

public interface ProductReportRepository extends JpaRepository<Product, Long> {

       // Get products by category with stock info
       @Query("SELECT c.categoryName, COUNT(p) as productCount, " +
                     "COALESCE(SUM(pv.stock), 0) as totalStock, " +
                     "SUM(CASE WHEN pv.stock > 0 THEN 1 ELSE 0 END) as activeCount " +
                     "FROM Product p " +
                     "JOIN p.category c " +
                     "LEFT JOIN p.variants pv " +
                     "GROUP BY c.categoryName")
       List<Object[]> getProductsByCategory();

       // Get top selling products within a date range
       @Query("SELECT p.productName, c.categoryName, pv.price, pv.stock, " +
                     "COALESCE(SUM(CASE WHEN o.orderId IS NOT NULL THEN oi.quantity ELSE 0 END), 0) as soldQuantity, " +
                     "COALESCE(SUM(CASE WHEN o.orderId IS NOT NULL THEN oi.quantity * oi.price ELSE 0 END), 0) as revenue " +
                     "FROM Product p " +
                     "JOIN p.category c " +
                     "JOIN p.variants pv " +
                     "LEFT JOIN OrderItem oi ON pv.variantId = oi.productVariant.variantId " +
                     "LEFT JOIN Order o ON oi.order = o AND o.status IN :statuses " +
                     "AND o.orderDate BETWEEN :startDate AND :endDate " +
                     "GROUP BY p.productName, c.categoryName, pv.price, pv.stock " +
                     "ORDER BY soldQuantity DESC")
       List<Object[]> getTopSellingProducts(@Param("statuses") List<OrderStatus> statuses,
                                           @Param("startDate") java.time.LocalDate startDate,
                                           @Param("endDate") java.time.LocalDate endDate);

       // Get all products with sales info within a date range
       @Query("SELECT p.productName, c.categoryName, pv.price, pv.stock, " +
                     "COALESCE(SUM(CASE WHEN o.orderId IS NOT NULL THEN oi.quantity ELSE 0 END), 0) as soldQuantity, " +
                     "COALESCE(SUM(CASE WHEN o.orderId IS NOT NULL THEN oi.quantity * oi.price ELSE 0 END), 0) as revenue " +
                     "FROM Product p " +
                     "JOIN p.category c " +
                     "JOIN p.variants pv " +
                     "LEFT JOIN OrderItem oi ON pv.variantId = oi.productVariant.variantId " +
                     "LEFT JOIN Order o ON oi.order = o AND o.status IN :statuses " +
                     "AND o.orderDate BETWEEN :startDate AND :endDate " +
                     "GROUP BY p.productName, c.categoryName, pv.price, pv.stock " +
                     "ORDER BY p.productName")
       List<Object[]> getAllProductsWithSales(@Param("statuses") List<OrderStatus> statuses,
                                             @Param("startDate") java.time.LocalDate startDate,
                                             @Param("endDate") java.time.LocalDate endDate);

       // Get product summary statistics
       @Query("SELECT COUNT(DISTINCT p), " +
                     "SUM(CASE WHEN pv.stock > 0 THEN 1 ELSE 0 END), " +
                     "SUM(CASE WHEN pv.stock = 0 THEN 1 ELSE 0 END), " +
                     "SUM(CASE WHEN pv.stock > 0 AND pv.stock < 10 THEN 1 ELSE 0 END) " +
                     "FROM Product p " +
                     "LEFT JOIN p.variants pv")
       List<Object[]> getProductSummary();
}
