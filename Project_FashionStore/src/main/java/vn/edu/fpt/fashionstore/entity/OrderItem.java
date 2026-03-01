package vn.edu.fpt.fashionstore.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "orderitem")
public class OrderItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long orderItemId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant productVariant;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "price")
    private Double totalPrice;
    
    // Constructor
    public OrderItem() {}
    
    public OrderItem(Order order, ProductVariant productVariant, Integer quantity) {
        this.order = order;
        this.productVariant = productVariant;
        this.quantity = (quantity != null) ? quantity : 0;
        // Tính totalPrice trực tiếp từ productVariant price * quantity
        this.totalPrice = (productVariant != null && productVariant.getPrice() != null) 
            ? productVariant.getPrice() * this.quantity 
            : 0.0;
    }
    
    // Getters and Setters
    public Long getOrderItemId() {
        return orderItemId;
    }
    
    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }
    
    public Order getOrder() {
        return order;
    }
    
    public void setOrder(Order order) {
        this.order = order;
    }
    
    public ProductVariant getProductVariant() {
        return productVariant;
    }
    
    public void setProductVariant(ProductVariant productVariant) {
        this.productVariant = productVariant;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        // Auto calculate total price when quantity changes
        // Không còn unitPrice nên không tính lại ở đây
    }
    
    
    public Double getTotalPrice() {
        return totalPrice;
    }
    
    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }
    
    // Business methods
    public String getProductName() {
        if (productVariant == null) return "N/A";
        if (productVariant.getProduct() == null) return "N/A";
        return productVariant.getProduct().getProductName();
    }
    
    public String getProductImage() {
        if (productVariant == null) return "";
        return productVariant.getImageUrl() != null ? productVariant.getImageUrl() : "";
    }
    
    public String getSizeName() {
        if (productVariant == null) return "N/A";
        if (productVariant.getCategorySize() == null) return "N/A";
        return productVariant.getCategorySize().getSizeName();
    }
    
    public String getColorName() {
        if (productVariant == null) return "N/A";
        if (productVariant.getColor() == null) return "N/A";
        return productVariant.getColor().getColorName();
    }
    
    @Override
    public String toString() {
        return "OrderItem{" +
                "orderItemId=" + orderItemId +
                ", quantity=" + quantity +
                ", totalPrice=" + totalPrice +
                '}';
    }
}
