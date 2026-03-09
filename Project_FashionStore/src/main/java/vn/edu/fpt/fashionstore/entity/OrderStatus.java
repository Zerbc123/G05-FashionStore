package vn.edu.fpt.fashionstore.entity;

public enum OrderStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    SHIPPING("Đang giao hàng"),       // Thêm trạng thái này cho khớp với bạn cùng nhóm
    COMPLETED("Đã giao thành công"),  // Thêm trạng thái này cho chức năng Review
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