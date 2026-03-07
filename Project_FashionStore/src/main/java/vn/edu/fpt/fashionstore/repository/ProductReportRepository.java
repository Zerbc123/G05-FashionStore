package vn.edu.fpt.fashionstore.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.fashionstore.entity.Product;

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

    // Get top selling products
    @Query("SELECT p.productName, c.categoryName, pv.price, pv.stock, " +
           "COALESCE(SUM(oi.quantity), 0) as soldQuantity, " +
           "COALESCE(SUM(oi.quantity * oi.price), 0) as revenue " +
           "FROM Product p " +
           "JOIN p.category c " +
           "JOIN p.variants pv " +
           "LEFT JOIN OrderItem oi ON pv.variantId = oi.variant.variantId " +
           "LEFT JOIN Orders o ON oi.order.orderId = o.orderId AND o.status = 'Completed' " +
           "GROUP BY p.productName, c.categoryName, pv.price, pv.stock " +
           "ORDER BY soldQuantity DESC")
    List<Object[]> getTopSellingProducts();

    // Get all products with sales info
    @Query("SELECT p.productName, c.categoryName, pv.price, pv.stock, " +
           "COALESCE(SUM(oi.quantity), 0) as soldQuantity, " +
           "COALESCE(SUM(oi.quantity * oi.price), 0) as revenue " +
           "FROM Product p " +
           "JOIN p.category c " +
           "JOIN p.variants pv " +
           "LEFT JOIN OrderItem oi ON pv.variantId = oi.variant.variantId " +
           "LEFT JOIN Orders o ON oi.order.orderId = o.orderId AND o.status = 'Completed' " +
           "GROUP BY p.productName, c.categoryName, pv.price, pv.stock " +
           "ORDER BY p.productName")
    List<Object[]> getAllProductsWithSales();

    // Get product summary
    @Query("SELECT COUNT(DISTINCT p) as totalProducts, " +
           "SUM(CASE WHEN pv.stock > 0 THEN 1 ELSE 0 END) as activeProducts, " +
           "SUM(CASE WHEN pv.stock = 0 THEN 1 ELSE 0 END) as outOfStockProducts, " +
           "SUM(CASE WHEN pv.stock > 0 AND pv.stock < 10 THEN 1 ELSE 0 END) as lowStockProducts " +
           "FROM Product p " +
           "JOIN p.variants pv")
    Object[] getProductSummary();
}
