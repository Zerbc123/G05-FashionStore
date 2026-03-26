package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.fashionstore.entity.Order;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.entity.OrderStatus;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END " +
           "FROM Order o JOIN o.orderItems oi JOIN oi.productVariant pv " +
           "WHERE o.customer = :customer AND pv.product.productId = :productId AND o.status = :status")
    boolean hasCustomerBoughtProduct(
           @Param("customer") Customer customer,
           @Param("productId") Long productId,
           @Param("status") OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o JOIN o.orderItems oi JOIN oi.productVariant pv " +
           "WHERE o.customer = :customer AND pv.product.productId = :productId " +
           "AND (o.status = vn.edu.fpt.fashionstore.entity.OrderStatus.CONFIRMED " +
           "OR o.status = vn.edu.fpt.fashionstore.entity.OrderStatus.COMPLETED)")
    long countSuccessfulPurchases(@Param("customer") Customer customer, @Param("productId") Long productId);

    List<Order> findByCustomerOrderByOrderDateDesc(Customer customer);
    List<Order> findByCustomerAndStatusOrderByOrderDateDesc(Customer customer, OrderStatus status);
    List<Order> findByStatusOrderByOrderDateDesc(OrderStatus status);
    List<Order> findAll();

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.customer ORDER BY o.orderDate DESC")
    List<Order> findOrdersForAdmin();

    long countByStatus(OrderStatus status);
    long countByCustomer(Customer customer);

    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate ORDER BY o.orderDate DESC")
    List<Order> findByOrderDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT o FROM Order o WHERE o.customer = :customer AND o.orderDate BETWEEN :startDate AND :endDate ORDER BY o.orderDate DESC")
    List<Order> findByCustomerAndOrderDateBetween(
            @Param("customer") Customer customer,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    boolean existsByOrderIdAndCustomer(Long orderId, Customer customer);
    List<Order> findTop5ByCustomerOrderByOrderDateDesc(Customer customer);
    List<Order> findByStatusInOrderByOrderDateDesc(List<OrderStatus> statuses);

    @Query("SELECT o FROM Order o " +
           "LEFT JOIN FETCH o.orderItems " +
           "LEFT JOIN FETCH o.customer " +
           "WHERE o.orderId = :orderId")
    Order findByOrderIdWithDetails(@Param("orderId") Long orderId);

    List<Order> findByCustomer_CustomerIdOrderByOrderDateDesc(Long customerId);

    @Query("SELECT o FROM Order o " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.productVariant pv " +
           "LEFT JOIN FETCH pv.product " +
           "WHERE o.orderId = :id")
    Order findOrderWithItems(@Param("id") Long id);

    @Query("SELECT o.orderDate, COALESCE(SUM(o.totalAmount), 0), COUNT(o) " +
           "FROM Order o " +
           "WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status IN :statuses " +
           "GROUP BY o.orderDate " +
           "ORDER BY o.orderDate")
    List<Object[]> getDailyRevenue(@Param("startDate") LocalDate startDate, 
                                   @Param("endDate") LocalDate endDate, 
                                   @Param("statuses") List<OrderStatus> statuses);

    @Query("SELECT YEAR(o.orderDate), MONTH(o.orderDate), COALESCE(SUM(o.totalAmount), 0) " +
           "FROM Order o " +
           "WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status IN :statuses " +
           "GROUP BY YEAR(o.orderDate), MONTH(o.orderDate) " +
           "ORDER BY YEAR(o.orderDate), MONTH(o.orderDate)")
    List<Object[]> getMonthlyRevenue(@Param("startDate") LocalDate startDate, 
                                     @Param("endDate") LocalDate endDate, 
                                     @Param("statuses") List<OrderStatus> statuses);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) " +
           "FROM Order o " +
           "WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status IN :statuses")
    Double getTotalRevenue(@Param("startDate") LocalDate startDate, 
                          @Param("endDate") LocalDate endDate, 
                          @Param("statuses") List<OrderStatus> statuses);

    @Query("SELECT COUNT(o) " +
           "FROM Order o " +
           "WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status IN :statuses")
    Long getTotalOrders(@Param("startDate") LocalDate startDate, 
                        @Param("endDate") LocalDate endDate, 
                        @Param("statuses") List<OrderStatus> statuses);
    
    boolean existsByVoucher_VoucherId(Integer voucherId);
}
