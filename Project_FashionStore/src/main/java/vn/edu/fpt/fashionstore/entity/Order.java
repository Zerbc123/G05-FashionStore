package vn.edu.fpt.fashionstore.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;
    
    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    // GIỮ NGUYÊN ENUM CỦA BẠN
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private OrderStatus status;

    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    @Transient
    private String orderCode;

    @Transient
    private String deliveryAddress;

    @Column(name = "confirmed_by", length = 100, columnDefinition = "NVARCHAR(100)")
    private String confirmedBy;

    @Column(name = "confirmed_date")
    private java.time.LocalDateTime confirmedDate;

    @Column(name = "cancelled_by", length = 100, columnDefinition = "NVARCHAR(100)")
    private String cancelledBy;

    @Column(name = "cancelled_date")
    private java.time.LocalDateTime cancelledDate;

    @Column(name = "cancellation_reason", length = 500, columnDefinition = "NVARCHAR(500)")
    private String cancellationReason;

    @Column(name = "payment_status", length = 20)
    private String paymentStatus;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems;

    // Constructor
    public Order() {
        this.orderDate = LocalDate.now();
        this.status = OrderStatus.PENDING;
    }

    public Order(Customer customer, Double totalAmount) {
        this();
        this.customer = customer;
        this.totalAmount = totalAmount;
    }

    // --- Getters and Setters ---
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public List<OrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public Voucher getVoucher() { return voucher; }
    public void setVoucher(Voucher voucher) { this.voucher = voucher; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(String confirmedBy) { this.confirmedBy = confirmedBy; }

    public java.time.LocalDateTime getConfirmedDate() { return confirmedDate; }
    public void setConfirmedDate(java.time.LocalDateTime confirmedDate) { this.confirmedDate = confirmedDate; }

    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }

    public java.time.LocalDateTime getCancelledDate() { return cancelledDate; }
    public void setCancelledDate(java.time.LocalDateTime cancelledDate) { this.cancelledDate = cancelledDate; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    // --- Business methods cũ của bạn ---
    public boolean canBeCancelled() {
        // Không cho hủy đơn đã thanh toán MoMo
        if ("MOMO".equalsIgnoreCase(paymentMethod) && "PAID".equalsIgnoreCase(paymentStatus)) {
            return false;
        }
        
        // Cho phép hủy đơn PENDING hoặc CONFIRMED (trong vòng 1 phút)
        // COMPLETED thì không cho hủy (đã giao hàng thành công)
        if (status == OrderStatus.PENDING) {
            return true;
        }
        
        if (status == OrderStatus.CONFIRMED) {
            // Chỉ cho hủy nếu chưa quá 1 phút kể từ khi confirm
            if (confirmedDate != null) {
                java.time.Duration duration = java.time.Duration.between(confirmedDate, java.time.LocalDateTime.now());
                return duration.toMinutes() < 1; // Chỉ cho hủy trong vòng 1 phút
            }
            return true; // Nếu chưa có confirmedDate thì vẫn cho hủy
        }
        
        return false; // COMPLETED và CANCELLED không cho hủy
    }
    
    // Kiểm tra xem đơn hàng có thể tự động chuyển sang COMPLETED không
    public boolean shouldAutoComplete() {
        if (status == OrderStatus.CONFIRMED && confirmedDate != null) {
            java.time.Duration duration = java.time.Duration.between(confirmedDate, java.time.LocalDateTime.now());
            return duration.toMinutes() >= 1; // Sau 24h tự động COMPLETED
        }
        return false;
    }

    public boolean canBeConfirmed() {
        return status == OrderStatus.PENDING;
    }

    public void confirm(String confirmedBy) {
        if (!canBeConfirmed()) {
            throw new RuntimeException("Đơn hàng không thể xác nhận ở trạng thái: " + status);
        }
        this.status = OrderStatus.CONFIRMED;
        this.confirmedBy = confirmedBy;
        this.confirmedDate = java.time.LocalDateTime.now();
    }

    public void cancel(String cancelledBy, String reason) {
        if (!canBeCancelled()) {
            throw new RuntimeException("Đơn hàng không thể hủy ở trạng thái: " + status);
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelledBy = cancelledBy;
        this.cancelledDate = java.time.LocalDateTime.now();
        this.cancellationReason = reason;
    }


    public String getOrderCode() {
        return "#" + String.format("%06d", orderId);
    }

    // public String getDeliveryAddress() {
    //     return customer != null ? customer.getAddress() : "";
    // }

    // --- THÊM HÀM NÀY CỦA BẠN CÙNG NHÓM ---
    public Double getCalculatedTotal() {
        if (orderItems == null || orderItems.isEmpty()) {
            return 0.0;
        }
        double total = 0;
        for (OrderItem item : orderItems) {
            // Gọi getTotalPrice() thay vì getPrice().
            // Vì totalPrice của bạn đã nhân sẵn với quantity rồi nên không cần nhân nữa.
            total += (item.getTotalPrice() != null) ? item.getTotalPrice() : 0.0;
        }
        return total;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId=" + orderId +
                ", status=" + status +
                '}';
    }
}