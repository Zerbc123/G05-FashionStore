package vn.edu.fpt.fashionstore.entity;

public enum ReturnStatus {
    PENDING,    // Đang chờ Admin xử lý
    APPROVED,   // Admin đã đồng ý, chờ khách gửi hàng về kho
    REJECTED,   // Admin từ chối yêu cầu trả hàng
    COMPLETED   // Shop đã nhận lại hàng và hoàn tiền thành công
}