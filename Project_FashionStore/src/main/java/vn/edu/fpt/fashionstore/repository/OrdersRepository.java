package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.fashionstore.entity.Orders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Long> {

    @Query("SELECT o.orderDate, SUM(o.totalAmount) as revenue, COUNT(o) as orders " +
           "FROM Orders o " +
           "WHERE o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.status = 'Completed' " +
           "GROUP BY o.orderDate " +
           "ORDER BY o.orderDate")
    List<Object[]> getDailyRevenue(@Param("startDate") LocalDate startDate, 
                                   @Param("endDate") LocalDate endDate);

    @Query("SELECT CONCAT(YEAR(o.orderDate), '-', RIGHT(CONCAT('0', CAST(MONTH(o.orderDate) AS STRING)), 2)) as monthYear, SUM(o.totalAmount) as revenue " +
           "FROM Orders o " +
           "WHERE o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.status = 'Completed' " +
           "GROUP BY YEAR(o.orderDate), MONTH(o.orderDate) " +
           "ORDER BY YEAR(o.orderDate), MONTH(o.orderDate)")
    List<Object[]> getMonthlyRevenue(@Param("startDate") LocalDate startDate, 
                                     @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(o.totalAmount) " +
           "FROM Orders o " +
           "WHERE o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.status = 'Completed'")
    BigDecimal getTotalRevenue(@Param("startDate") LocalDate startDate, 
                              @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(o) " +
           "FROM Orders o " +
           "WHERE o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.status = 'Completed'")
    Long getTotalOrders(@Param("startDate") LocalDate startDate, 
                       @Param("endDate") LocalDate endDate);
}
