-- Tạo bảng support_chat để lưu trữ tin nhắn hội thoại
USE fashion_store;
GO

-- Kiểm tra xem bảng support_chat đã tồn tại chưa
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'support_chat')
BEGIN
    -- Tạo bảng support_chat
    CREATE TABLE support_chat (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        support_request_id BIGINT NOT NULL,
        sender_type NVARCHAR(20) NOT NULL, -- 'CUSTOMER' hoặc 'STAFF'
        sender_name NVARCHAR(100) NOT NULL,
        message_content NVARCHAR(MAX) NOT NULL,
        sent_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        is_read BIT NOT NULL DEFAULT 0, -- 0: chưa đọc, 1: đã đọc
        staff_id INT NULL -- ID của nhân viên nếu sender là STAFF
    );
    
    -- Thêm foreign key constraint
    ALTER TABLE support_chat 
    ADD CONSTRAINT FK_support_chat_support_request 
    FOREIGN KEY (support_request_id) REFERENCES support_request(id) 
    ON DELETE CASCADE;
    
    -- Thêm indexes cho performance
    CREATE INDEX IX_support_chat_support_request_id ON support_chat(support_request_id);
    CREATE INDEX IX_support_chat_sent_at ON support_chat(sent_at);
    CREATE INDEX IX_support_chat_sender_type ON support_chat(sender_type);
    CREATE INDEX IX_support_chat_is_read ON support_chat(is_read);
    
    PRINT 'Đã tạo bảng support_chat thành công!';
END
ELSE
BEGIN
    PRINT 'Bảng support_chat đã tồn tại!';
END
