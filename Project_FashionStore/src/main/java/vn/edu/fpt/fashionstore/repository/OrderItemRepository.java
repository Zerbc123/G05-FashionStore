package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.fashionstore.entity.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT p.productName, SUM(oi.quantity) as totalSold, SUM(oi.quantity * oi.price) as revenue, c.categoryName " +
           "FROM OrderItem oi " +
           "JOIN oi.variant v " +
           "JOIN v.product p " +
           "JOIN p.category c " +
           "JOIN oi.order o " +
           "WHERE o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.status IN ('Completed', 'Pending') " +
           "GROUP BY p.productId, p.productName, c.categoryName " +
           "ORDER BY totalSold DESC")
    List<Object[]> getBestSellingProducts(@Param("startDate") LocalDate startDate, 
                                          @Param("endDate") LocalDate endDate);

    @Query("SELECT c.categoryName, SUM(oi.quantity) as totalSold, SUM(oi.quantity * oi.price) as revenue " +
           "FROM OrderItem oi " +
           "JOIN oi.variant v " +
           "JOIN v.product p " +
           "JOIN p.category c " +
           "JOIN oi.order o " +
           "WHERE o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.status IN ('Completed', 'Pending') " +
           "GROUP BY c.categoryId, c.categoryName " +
           "ORDER BY revenue DESC")
    List<Object[]> getBestSellingCategories(@Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate);
}
