package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.fashionstore.entity.OrderItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.fashionstore.entity.Order;
import vn.edu.fpt.fashionstore.entity.OrderItem;
import vn.edu.fpt.fashionstore.entity.OrderStatus;
import vn.edu.fpt.fashionstore.entity.ProductVariant;

import java.time.LocalDate;
import java.util.List;
import java.time.LocalDate;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    // Check if any order item exists for a specific variant
    boolean existsByProductVariantVariantId(int variantId);
    
    // Get all order items for a specific variant
    List<OrderItem> findByProductVariantVariantId(int variantId);
    
    // Count order items for a specific variant
    long countByProductVariantVariantId(int variantId);
    // Lấy danh sách order items theo đơn hàng
    List<OrderItem> findByOrderOrderByOrderItemIdAsc(Order order);
    
    // Lấy order items theo sản phẩm variant
    List<OrderItem> findByProductVariant(ProductVariant productVariant);
    
    // Đếm số lượng sản phẩm đã bán theo variant
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.productVariant = :variant AND oi.order.status = 'CONFIRMED'")
    Long countSoldQuantityByVariant(@Param("variant") ProductVariant variant);
    
    // Đếm số lượng sản phẩm đã bán theo variant (tất cả các trạng thái trừ hủy)
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.productVariant = :variant AND oi.order.status != 'CANCELLED'")
    Long countTotalSoldQuantityByVariant(@Param("variant") ProductVariant variant);
    
    // Tính doanh thu theo đơn hàng
    @Query("SELECT COALESCE(SUM(oi.totalPrice), 0) FROM OrderItem oi WHERE oi.order = :order")
    Double calculateOrderTotal(@Param("order") Order order);
    
    // Tính doanh thu theo khoảng thời gian
    @Query("SELECT COALESCE(SUM(oi.totalPrice), 0) FROM OrderItem oi WHERE oi.order.orderDate BETWEEN :startDate AND :endDate AND oi.order.status != 'CANCELLED'")
    Double calculateRevenueBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // Lấy top sản phẩm bán chạy
    @Query("SELECT oi.productVariant, SUM(oi.quantity) as totalSold FROM OrderItem oi " +
           "WHERE oi.order.status != 'CANCELLED' " +
           "GROUP BY oi.productVariant " +
           "ORDER BY totalSold DESC")
    List<Object[]> findTopSellingVariants();
    
    // Lấy order items theo danh sách đơn hàng
    List<OrderItem> findByOrderIn(List<Order> orders);

    // Kiểm tra order item có thuộc đơn hàng không
    boolean existsByOrderItemIdAndOrder(Long orderItemId, Order order);

    // ==========================================
    // REPORT METHODS
    // ==========================================

    // Get best selling products with details
    @Query("SELECT pv.product.productName, SUM(oi.quantity), SUM(oi.totalPrice), pv.product.category.categoryName " +
           "FROM OrderItem oi " +
           "JOIN oi.productVariant pv " +
           "JOIN oi.order o " +
           "WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status IN :statuses " +
           "GROUP BY pv.product.productName, pv.product.category.categoryName " +
           "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> getBestSellingProducts(@Param("startDate") LocalDate startDate, 
                                          @Param("endDate") LocalDate endDate, 
                                          @Param("statuses") List<OrderStatus> statuses);

    // Get best selling categories
    @Query("SELECT pv.product.category.categoryName, SUM(oi.quantity), SUM(oi.totalPrice) " +
           "FROM OrderItem oi " +
           "JOIN oi.productVariant pv " +
           "JOIN oi.order o " +
           "WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status IN :statuses " +
           "GROUP BY pv.product.category.categoryName " +
           "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> getBestSellingCategories(@Param("startDate") LocalDate startDate, 
                                            @Param("endDate") LocalDate endDate, 
                                            @Param("statuses") List<OrderStatus> statuses);
}
