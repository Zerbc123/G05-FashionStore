package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.fashionstore.entity.Order;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

       // ==========================================
       // HÀM KIỂM TRA QUYỀN ĐÁNH GIÁ (DÀNH CHO REVIEW)
       // ==========================================
       @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END " +
                     "FROM Order o JOIN o.orderItems oi JOIN oi.productVariant pv " +
                     "WHERE o.customer = :customer AND pv.product.productId = :productId AND o.status = :status")
       boolean hasCustomerBoughtProduct(
                     @Param("customer") Customer customer,
                     @Param("productId") Long productId,
                     @Param("status") OrderStatus status);

       // ==========================================
       // CÁC HÀM CỦA BẠN (ĐƯỢC GIỮ NGUYÊN 100%)
       // ==========================================

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
            @Param("endDate") LocalDate endDate);

       // Kiểm tra khách hàng có sở hữu đơn hàng không
       boolean existsByOrderIdAndCustomer(Long orderId, Customer customer);

       // Tìm đơn hàng gần đây của khách hàng
       List<Order> findTop5ByCustomerOrderByOrderDateDesc(Customer customer);

       // Tìm đơn hàng theo nhiều trạng thái
       List<Order> findByStatusInOrderByOrderDateDesc(List<OrderStatus> statuses);
    // Tìm đơn hàng theo ID kèm thông tin chi tiết
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.orderItems " +
            "LEFT JOIN FETCH o.customer " +
            "WHERE o.orderId = :orderId")
    Order findByOrderIdWithDetails(@Param("orderId") Long orderId);

    // Lấy danh sách đơn hàng của khách hàng theo customerId
    List<Order> findByCustomer_CustomerIdOrderByOrderDateDesc(Long customerId);

    // Lấy chi tiết 1 đơn hàng kèm theo danh sách sản phẩm (Dùng cho giao diện Admin & Xuất PDF)
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.productVariant pv " +
            "LEFT JOIN FETCH pv.product " +
            "WHERE o.orderId = :id")
    Order findOrderWithItems(@Param("id") Long id);

    // ==========================================
    // REPORT METHODS
    // ==========================================

    // Get daily revenue data
    @Query("SELECT o.orderDate, COALESCE(SUM(o.totalAmount), 0), COUNT(o) " +
           "FROM Order o " +
           "WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status IN :statuses " +
           "GROUP BY o.orderDate " +
           "ORDER BY o.orderDate")
    List<Object[]> getDailyRevenue(@Param("startDate") LocalDate startDate, 
                                   @Param("endDate") LocalDate endDate, 
                                   @Param("statuses") List<OrderStatus> statuses);

    // Get monthly revenue data
    @Query("SELECT YEAR(o.orderDate), MONTH(o.orderDate), COALESCE(SUM(o.totalAmount), 0) " +
           "FROM Order o " +
           "WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status IN :statuses " +
           "GROUP BY YEAR(o.orderDate), MONTH(o.orderDate) " +
           "ORDER BY YEAR(o.orderDate), MONTH(o.orderDate)")
    List<Object[]> getMonthlyRevenue(@Param("startDate") LocalDate startDate, 
                                     @Param("endDate") LocalDate endDate, 
                                     @Param("statuses") List<OrderStatus> statuses);

    // Get total revenue
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) " +
           "FROM Order o " +
           "WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status IN :statuses")
    Double getTotalRevenue(@Param("startDate") LocalDate startDate, 
                          @Param("endDate") LocalDate endDate, 
                          @Param("statuses") List<OrderStatus> statuses);

    // Get total orders count
    @Query("SELECT COUNT(o) " +
           "FROM Order o " +
           "WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status IN :statuses")
    Long getTotalOrders(@Param("startDate") LocalDate startDate, 
                        @Param("endDate") LocalDate endDate, 
                        @Param("statuses") List<OrderStatus> statuses);
}
