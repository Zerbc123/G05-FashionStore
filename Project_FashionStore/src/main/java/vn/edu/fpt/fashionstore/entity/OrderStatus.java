package vn.edu.fpt.fashionstore.entity;

public enum OrderStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    SHIPPING("Đang giao hàng"),
    COMPLETED("Đã hoàn thành"),
    CANCELLED("Đã hủy"),
    REFUNDED("Đã hoàn tiền");
    
    private final String displayName;
    
    OrderStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
