package vn.edu.fpt.fashionstore.entity;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "Return_Requests")
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_id")
    private Long returnId;

    // Liên kết với đơn hàng nào đang bị yêu cầu trả
    // Dùng OneToOne vì thông thường 1 đơn hàng chỉ được tạo 1 yêu cầu trả (cho toàn bộ đơn)
    @OneToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Ai là người yêu cầu
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // Lý do trả hàng (VD: Hàng lỗi, Không vừa size, Giao sai mẫu...)
    @Column(name = "reason", length = 255)
    private String reason;

    // Lời giải thích chi tiết của khách hàng
    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    // Trạng thái của yêu cầu này
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ReturnStatus status;

    // Ngày khách bấm gửi yêu cầu
    @Column(name = "request_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date requestDate;

    // Lời nhắn của Admin gửi cho khách (khi từ chối hoặc hướng dẫn gửi hàng)
    @Column(name = "admin_note", columnDefinition = "NVARCHAR(MAX)")
    private String adminNote;

    // --- CONSTRUCTORS ---
    public ReturnRequest() {
    }

    // --- GETTERS & SETTERS (Bạn có thể dùng Generate của IntelliJ hoặc Lombok @Data) ---

    public Long getReturnId() { return returnId; }
    public void setReturnId(Long returnId) { this.returnId = returnId; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ReturnStatus getStatus() { return status; }
    public void setStatus(ReturnStatus status) { this.status = status; }

    public Date getRequestDate() { return requestDate; }
    public void setRequestDate(Date requestDate) { this.requestDate = requestDate; }

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
}