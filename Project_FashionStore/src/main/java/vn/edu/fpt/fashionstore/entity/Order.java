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
    @Temporal(TemporalType.DATE)
    private Date orderDate;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    // GIỮ NGUYÊN ENUM CỦA BẠN
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private OrderStatus status;

    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    // THÊM BIẾN NÀY ĐỂ ĐỒNG BỘ VỚI CODE BẠN CỦA BẠN
    @Column(name = "payment_status")
    private String paymentStatus;

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

    // --- Getters and Setters ---
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public Date getOrderDate() { return orderDate; }
    public void setOrderDate(Date orderDate) { this.orderDate = orderDate; }

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

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    // --- Business methods cũ của bạn ---
    public boolean canBeCancelled() {
        return status == OrderStatus.PENDING;
    }

    public boolean canBeConfirmed() {
        return status == OrderStatus.PENDING;
    }

    public void confirm(String confirmedBy) {
        if (!canBeConfirmed()) {
            throw new RuntimeException("Đơn hàng không thể xác nhận ở trạng thái: " + status);
        }
        this.status = OrderStatus.CONFIRMED;
        System.out.println("Order #" + orderId + " confirmed by: " + confirmedBy);
    }

    public void cancel(String cancelledBy, String reason) {
        if (!canBeCancelled()) {
            throw new RuntimeException("Đơn hàng không thể hủy ở trạng thái: " + status);
        }
        this.status = OrderStatus.CANCELLED;
        System.out.println("Order #" + orderId + " cancelled by: " + cancelledBy + ", reason: " + reason);
    }

    public String getOrderCode() {
        return "#" + String.format("%06d", orderId);
    }

    public String getDeliveryAddress() {
        return customer != null ? customer.getAddress() : "";
    }

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