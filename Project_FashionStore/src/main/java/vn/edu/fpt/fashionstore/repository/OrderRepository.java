package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.entity.Order;
import vn.edu.fpt.fashionstore.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    // Lấy danh sách đơn hàng của khách hàng
    List<Order> findByCustomerOrderByOrderDateDesc(Customer customer);
    
    // Lấy đơn hàng của khách hàng theo trạng thái
    List<Order> findByCustomerAndStatusOrderByOrderDateDesc(Customer customer, OrderStatus status);
    
    // Lấy tất cả đơn hàng theo trạng thái (cho admin/staff)
    List<Order> findByStatusOrderByOrderDateDesc(OrderStatus status);
    
    // Lấy tất cả đơn hàng (cho admin/staff)
    List<Order> findAll();
    
    // Đếm số đơn hàng theo trạng thái
    long countByStatus(OrderStatus status);
    
    // Đếm số đơn hàng của khách hàng
    long countByCustomer(Customer customer);
    
    // Tìm đơn hàng theo khoảng thời gian
    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate ORDER BY o.orderDate DESC")
    List<Order> findByOrderDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // Tìm đơn hàng của khách hàng theo khoảng thời gian
    @Query("SELECT o FROM Order o WHERE o.customer = :customer AND o.orderDate BETWEEN :startDate AND :endDate ORDER BY o.orderDate DESC")
    List<Order> findByCustomerAndOrderDateBetween(
        @Param("customer") Customer customer, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate
    );
    
    // Kiểm tra khách hàng có sở hữu đơn hàng không
    boolean existsByOrderIdAndCustomer(Long orderId, Customer customer);
    
    // Tìm đơn hàng gần đây của khách hàng
    List<Order> findTop5ByCustomerOrderByOrderDateDesc(Customer customer);
    
    // Tìm đơn hàng theo nhiều trạng thái
    List<Order> findByStatusInOrderByOrderDateDesc(List<OrderStatus> statuses);
    
    // === Revenue Report Methods (merged from OrdersRepository) ===
    
    @Query("SELECT o.orderDate, SUM(o.totalAmount) as revenue, COUNT(o) as orders " +
           "FROM Order o " +
           "WHERE o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.status IN ('Completed', 'Pending') " +
           "GROUP BY o.orderDate " +
           "ORDER BY o.orderDate")
    List<Object[]> getDailyRevenue(@Param("startDate") LocalDate startDate, 
                                   @Param("endDate") LocalDate endDate);

    @Query("SELECT CONCAT(YEAR(o.orderDate), '-', RIGHT(CONCAT('0', CAST(MONTH(o.orderDate) AS string)), 2)) as monthYear, SUM(o.totalAmount) as revenue " +
           "FROM Order o " +
           "WHERE o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.status IN ('Completed', 'Pending') " +
           "GROUP BY YEAR(o.orderDate), MONTH(o.orderDate) " +
           "ORDER BY YEAR(o.orderDate), MONTH(o.orderDate)")
    List<Object[]> getMonthlyRevenue(@Param("startDate") LocalDate startDate, 
                                     @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(o.totalAmount) " +
           "FROM Order o " +
           "WHERE o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.status IN ('Completed', 'Pending')")
    Double getTotalRevenue(@Param("startDate") LocalDate startDate, 
                          @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(o) " +
           "FROM Order o " +
           "WHERE o.orderDate BETWEEN :startDate AND :endDate " +
           "AND o.status IN ('Completed', 'Pending')")
    Long getTotalOrders(@Param("startDate") LocalDate startDate, 
                       @Param("endDate") LocalDate endDate);
}
