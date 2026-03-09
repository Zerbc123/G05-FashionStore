package vn.edu.fpt.fashionstore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.fashionstore.entity.*;
import vn.edu.fpt.fashionstore.repository.OrderRepository;
import vn.edu.fpt.fashionstore.repository.OrderItemRepository;
import vn.edu.fpt.fashionstore.service.CartService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    @Autowired
    private CartService cartService;

    @Transactional
    public long countSuccessfulPurchases(Customer customer, Long productId) {
        // Đếm tất cả OrderItem của khách hàng này, thuộc đơn hàng COMPLETED và đúng mã sản phẩm
        return orderRepository.findAll().stream()
                .filter(o -> o.getCustomer().getCustomerId() == customer.getCustomerId()
                        && o.getStatus() == OrderStatus.COMPLETED)
                .flatMap(o -> o.getOrderItems().stream())
                .filter(oi -> oi.getProductVariant().getProduct().getProductId().equals(productId))
                .count();
    }

    // =======================================================
    // 1. TẠO ĐƠN HÀNG TỪ GIỎ HÀNG
    // =======================================================
    @Transactional
    public Order createOrderFromCart(Customer customer, String deliveryAddress) {
        try {
            // Kiểm tra customer null
            if (customer == null) {
                throw new RuntimeException("Customer không được để trống!");
            }
            
            // Kiểm tra deliveryAddress
            if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
                throw new RuntimeException("Địa chỉ giao hàng không được để trống!");
            }
            
            // Lấy các sản phẩm trong giỏ hàng
            List<CartItem> cartItems = cartService.getCartItems(customer);
            
            if (cartItems.isEmpty()) {
                throw new RuntimeException("Giỏ hàng trống, không thể tạo đơn hàng!");
            }

            // Tính tổng tiền
            double totalAmount = cartItems.stream()
                .mapToDouble(item -> item.getProductVariant().getPrice() * item.getQuantity())
                .sum();

            // Tạo đơn hàng mới với địa chỉ giao hàng và tài khoản
            Order order = new Order(customer, totalAmount);
            order.setShippingAddress(deliveryAddress);
            if (customer.getAccount() != null) {
                order.setAccount(customer.getAccount());
            }
            order = orderRepository.save(order);
            
            // Lưu OrderItem vào database
            List<OrderItem> orderItems = new ArrayList<>();
            for (CartItem cartItem : cartItems) {
                OrderItem orderItem = new OrderItem(order, cartItem.getProductVariant(), cartItem.getQuantity());
                orderItem = orderItemRepository.save(orderItem);
                orderItems.add(orderItem);
            }
            
            // Clear cart sau khi đã lưu OrderItem thành công
            cartService.clearCart(customer);

            return order;

        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo đơn hàng: " + e.getMessage(), e);
        }
    }

    // =======================================================
    // 2. LẤY THÔNG TIN ĐƠN HÀNG
    // =======================================================
    @Transactional(readOnly = true)
    public Order getOrderById(Long orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId);
        }
        return orderOpt.get();
    }

    // Method này không còn dùng vì database không có order_code
    // Có thể dùng getOrderById thay thế
    @Deprecated
    @Transactional(readOnly = true)
    public Order getOrderByCode(String orderCode) {
        // Nếu orderCode có dạng #123456, lấy số
        if (orderCode.startsWith("#")) {
            orderCode = orderCode.substring(1);
        }
        
        try {
            Long orderId = Long.parseLong(orderCode);
            return getOrderById(orderId);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Mã đơn hàng không hợp lệ: " + orderCode);
        }
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomer(Customer customer) {
        if (customer == null) {
            return Collections.emptyList();
        }
        return orderRepository.findByCustomerOrderByOrderDateDesc(customer);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomerAndStatus(Customer customer, OrderStatus status) {
        if (customer == null || status == null) {
            return Collections.emptyList();
        }
        return orderRepository.findByCustomerAndStatusOrderByOrderDateDesc(customer, status);
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        // Sort manually by orderDate descending
        orders.sort((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate()));
        return orders;
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatusOrderByOrderDateDesc(status);
    }

    // =======================================================
    // 3. CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG
    // =======================================================
    @Transactional
    public Order confirmOrder(Long orderId, String confirmedBy) {
        Order order = getOrderById(orderId);
        
        if (!order.canBeConfirmed()) {
            throw new RuntimeException("Đơn hàng này không thể xác nhận! Trạng thái hiện tại: " + order.getStatus().getDisplayName());
        }
        
        order.confirm(confirmedBy);
        order = orderRepository.save(order);
        
        System.out.println("[ORDER SERVICE] Order " + order.getOrderCode() + " confirmed by: " + confirmedBy);
        return order;
    }

    @Transactional
    public Order cancelOrder(Long orderId, String cancelledBy, String reason) {
        Order order = getOrderById(orderId);
        
        if (!order.canBeCancelled()) {
            throw new RuntimeException("Đơn hàng này không thể hủy! Trạng thái hiện tại: " + order.getStatus().getDisplayName());
        }
        
        order.cancel(cancelledBy, reason);
        order = orderRepository.save(order);
        
        System.out.println("[ORDER SERVICE] Order " + order.getOrderCode() + " cancelled by: " + cancelledBy + ", reason: " + reason);
        return order;
    }

    // =======================================================
    // 4. KIỂM TRA QUYỀN SỞ HỮU ĐƠN HÀNG
    // =======================================================
    @Transactional(readOnly = true)
    public boolean isOrderOwnedByCustomer(Long orderId, Customer customer) {
        return orderRepository.existsByOrderIdAndCustomer(orderId, customer);
    }

    // =======================================================
    // 5. THỐNG KÊ VÀ BÁO CÁO
    // =======================================================
    @Transactional(readOnly = true)
    public long countOrdersByStatus(OrderStatus status) {
        return orderRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public long countCustomerOrders(Customer customer) {
        return orderRepository.countByCustomer(customer);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByDateRange(Date startDate, Date endDate) {
        return orderRepository.findByOrderDateBetween(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public Double calculateRevenueBetween(Date startDate, Date endDate) {
        return orderItemRepository.calculateRevenueBetween(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getTopSellingProducts() {
        return orderItemRepository.findTopSellingVariants();
    }

    // =======================================================
    // 6. HELPER METHODS
    // =======================================================
    private String generateOrderCode() {
        // Tạo mã đơn hàng theo format: ORD + timestamp + random
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 1000);
        return "ORD" + timestamp + random;
    }

    // Lấy OrderItems từ database
    @Transactional(readOnly = true)
    public List<OrderItem> getOrderItemsByOrder(Order order) {
        System.out.println("[ORDER SERVICE] getOrderItemsByOrder called for Order ID: " + order.getOrderId());
        List<OrderItem> orderItems = orderItemRepository.findByOrderOrderByOrderItemIdAsc(order);
        System.out.println("[ORDER SERVICE] Found " + orderItems.size() + " OrderItems");
        return orderItems;
    }

    // TẠM THỜI: Dùng totalAmount từ order vì không có OrderItem
    @Transactional(readOnly = true)
    public Double calculateOrderTotal(Order order) {
        return order.getTotalAmount();
    }

    // =======================================================
    // 7. VALIDATION METHODS
    // =======================================================
    @Transactional(readOnly = true)
    public boolean canCustomerCancelOrder(Long orderId, Customer customer) {
        try {
            Order order = getOrderById(orderId);
            return isOrderOwnedByCustomer(orderId, customer) && order.canBeCancelled();
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean canStaffConfirmOrder(Long orderId) {
        try {
            Order order = getOrderById(orderId);
            return order.canBeConfirmed();
        } catch (Exception e) {
            return false;
        }
    }
}
