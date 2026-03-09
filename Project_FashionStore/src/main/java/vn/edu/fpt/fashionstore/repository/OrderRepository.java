package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.entity.Order;
import vn.edu.fpt.fashionstore.entity.OrderStatus;

import java.util.Date;
import java.util.List;
import java.util.Optional;

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
    List<Order> findByOrderDateBetween(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    // Tìm đơn hàng của khách hàng theo khoảng thời gian
    @Query("SELECT o FROM Order o WHERE o.customer = :customer AND o.orderDate BETWEEN :startDate AND :endDate ORDER BY o.orderDate DESC")
    List<Order> findByCustomerAndOrderDateBetween(
            @Param("customer") Customer customer,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );

    // Kiểm tra khách hàng có sở hữu đơn hàng không
    boolean existsByOrderIdAndCustomer(Long orderId, Customer customer);

    // Tìm đơn hàng gần đây của khách hàng
    List<Order> findTop5ByCustomerOrderByOrderDateDesc(Customer customer);

    // Tìm đơn hàng theo nhiều trạng thái
    List<Order> findByStatusInOrderByOrderDateDesc(List<OrderStatus> statuses);

    // ==========================================
    // HÀM BỔ SUNG TỪ CODE CỦA BẠN CÙNG NHÓM
    // ==========================================

    // Lấy chi tiết 1 đơn hàng kèm theo danh sách sản phẩm (Dùng cho giao diện Admin & Xuất PDF)
    @Query("""
            SELECT o FROM Order o
            LEFT JOIN FETCH o.orderItems oi
            LEFT JOIN FETCH oi.productVariant pv
            LEFT JOIN FETCH pv.product
            WHERE o.orderId = :id
            """)
    Order findOrderWithItems(@Param("id") Long id);
}