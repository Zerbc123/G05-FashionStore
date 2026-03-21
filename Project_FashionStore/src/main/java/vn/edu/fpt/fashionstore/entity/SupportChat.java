package vn.edu.fpt.fashionstore.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_chat")
public class SupportChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "support_request_id", nullable = false)
    private Long supportRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, columnDefinition = "NVARCHAR(20)")
    private SenderType senderType;

    @Column(name = "sender_name", nullable = false, columnDefinition = "NVARCHAR(100)")
    @NotBlank(message = "Tên người gửi không được để trống")
    @Size(max = 100, message = "Tên người gửi không quá 100 ký tự")
    private String senderName;

    @Column(name = "message_content", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    @Size(min = 1, message = "Nội dung tin nhắn phải có ít nhất 1 ký tự")
    private String messageContent;

    @Column(name = "sent_at", nullable = false, columnDefinition = "DATETIME2(0)")
    private LocalDateTime sentAt;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "staff_id")
    private Integer staffId;

    // Relationship với SupportRequest
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "support_request_id", insertable = false, updatable = false)
    private SupportRequest supportRequest;

    @PrePersist
    public void prePersist() {
        this.sentAt = LocalDateTime.now();
        this.isRead = false;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSupportRequestId() {
        return supportRequestId;
    }

    public void setSupportRequestId(Long supportRequestId) {
        this.supportRequestId = supportRequestId;
    }

    public SenderType getSenderType() {
        return senderType;
    }

    public void setSenderType(SenderType senderType) {
        this.senderType = senderType;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public Integer getStaffId() {
        return staffId;
    }

    public void setStaffId(Integer staffId) {
        this.staffId = staffId;
    }

    public SupportRequest getSupportRequest() {
        return supportRequest;
    }

    public void setSupportRequest(SupportRequest supportRequest) {
        this.supportRequest = supportRequest;
    }
}
