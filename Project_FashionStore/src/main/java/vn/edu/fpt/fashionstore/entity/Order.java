package vn.edu.fpt.fashionstore.entity;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;
    
    @Column(name = "order_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date orderDate;
    
    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private OrderStatus status;
    
    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems;
    
    // Constructor
    public Order() {
        this.orderDate = new Date();
        this.status = OrderStatus.PENDING;
    }
    
    public Order(Customer customer, Double totalAmount) {
        this();
        this.customer = customer;
        this.totalAmount = totalAmount;
    }
    
    // Getters and Setters
    public Long getOrderId() {
        return orderId;
    }
    
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    
    public Customer getCustomer() {
        return customer;
    }
    
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
    
    public Date getOrderDate() {
        return orderDate;
    }
    
    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }
    
    public Double getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    
    public List<OrderItem> getOrderItems() {
        return orderItems;
    }
    
    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }
    
    public String getShippingAddress() {
        return shippingAddress;
    }
    
    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
    
    public Account getAccount() {
        return account;
    }
    
    public void setAccount(Account account) {
        this.account = account;
    }
    
    // Business methods
    public boolean canBeCancelled() {
        return status == OrderStatus.PENDING;
    }
    
    public boolean canBeConfirmed() {
        return status == OrderStatus.PENDING;
    }
    
    // Method xác nhận đơn hàng - chỉ cập nhật status
    public void confirm(String confirmedBy) {
        if (!canBeConfirmed()) {
            throw new RuntimeException("Đơn hàng không thể xác nhận ở trạng thái: " + status);
        }
        this.status = OrderStatus.CONFIRMED;
        // Note: confirmedBy và confirmedDate không lưu vào DB vì không có column
        System.out.println("Order #" + orderId + " confirmed by: " + confirmedBy);
    }
    
    // Method hủy đơn hàng - chỉ cập nhật status
    public void cancel(String cancelledBy, String reason) {
        if (!canBeCancelled()) {
            throw new RuntimeException("Đơn hàng không thể hủy ở trạng thái: " + status);
        }
        this.status = OrderStatus.CANCELLED;
        // Note: cancelledBy, cancelledDate, cancellationReason không lưu vào DB vì không có column
        System.out.println("Order #" + orderId + " cancelled by: " + cancelledBy + ", reason: " + reason);
    }
    
    // Convenience method để tạo order code từ ID
    public String getOrderCode() {
        return "#" + String.format("%06d", orderId);
    }
    
    // Convenience method để lấy delivery address từ customer
    public String getDeliveryAddress() {
        return customer != null ? customer.getAddress() : "";
    }
    
    @Override
    public String toString() {
        return "Order{" +
                "orderId=" + orderId +
                ", status=" + status +
                '}';
    }
}
