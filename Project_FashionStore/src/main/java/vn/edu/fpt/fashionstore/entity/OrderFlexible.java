package vn.edu.fpt.fashionstore.entity;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "orders")
public class OrderFlexible {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @Column(name = "order_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date orderDate;
    
    @Column(name = "total_amount")
    private Double totalAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status;
    
    // Optional columns - sẽ được ignore nếu không tồn tại trong DB
    @Column(name = "order_code")
    private String orderCode;
    
    @Column(name = "delivery_address")
    private String deliveryAddress;
    
    @Column(name = "confirmed_by")
    private String confirmedBy;
    
    @Column(name = "confirmed_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date confirmedDate;
    
    @Column(name = "cancelled_by")
    private String cancelledBy;
    
    @Column(name = "cancelled_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date cancelledDate;
    
    @Column(name = "cancellation_reason")
    private String cancellationReason;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems;
    
    // Constructor
    public OrderFlexible() {
        this.orderDate = new Date();
        this.status = OrderStatus.PENDING;
    }
    
    // Constructor đơn giản - chỉ dùng các column cơ bản
    public OrderFlexible(Customer customer, Double totalAmount) {
        this();
        this.customer = customer;
        this.totalAmount = totalAmount;
    }
    
    // Constructor đầy đủ - dùng tất cả columns
    public OrderFlexible(Customer customer, String orderCode, String deliveryAddress, Double totalAmount) {
        this(customer, totalAmount);
        this.orderCode = orderCode;
        this.deliveryAddress = deliveryAddress;
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
    
    public String getOrderCode() {
        return orderCode;
    }
    
    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }
    
    public String getDeliveryAddress() {
        return deliveryAddress;
    }
    
    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }
    
    public String getConfirmedBy() {
        return confirmedBy;
    }
    
    public void setConfirmedBy(String confirmedBy) {
        this.confirmedBy = confirmedBy;
    }
    
    public Date getConfirmedDate() {
        return confirmedDate;
    }
    
    public void setConfirmedDate(Date confirmedDate) {
        this.confirmedDate = confirmedDate;
    }
    
    public String getCancelledBy() {
        return cancelledBy;
    }
    
    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }
    
    public Date getCancelledDate() {
        return cancelledDate;
    }
    
    public void setCancelledDate(Date cancelledDate) {
        this.cancelledDate = cancelledDate;
    }
    
    public String getCancellationReason() {
        return cancellationReason;
    }
    
    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
    
    public List<OrderItem> getOrderItems() {
        return orderItems;
    }
    
    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }
    
    // Business methods
    public void confirm(String confirmedBy) {
        this.status = OrderStatus.CONFIRMED;
        this.confirmedBy = confirmedBy;
        this.confirmedDate = new Date();
    }
    
    public void cancel(String cancelledBy, String reason) {
        this.status = OrderStatus.CANCELLED;
        this.cancelledBy = cancelledBy;
        this.cancelledDate = new Date();
        this.cancellationReason = reason;
    }
    
    public boolean canBeCancelled() {
        return status == OrderStatus.PENDING;
    }
    
    public boolean canBeConfirmed() {
        return status == OrderStatus.PENDING;
    }
    
    @Override
    public String toString() {
        return "OrderFlexible{" +
                "orderId=" + orderId +
                ", orderCode='" + orderCode + '\'' +
                ", status=" + status +
                '}';
    }
}
