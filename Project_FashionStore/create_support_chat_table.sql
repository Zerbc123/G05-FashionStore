-- Script để thêm bảng support_chat vào database fashion_store
-- Bảng này lưu trữ cuộc hội thoại giữa khách hàng và nhân viên hỗ trợ
-- Chạy script này trong SQL Server Management Studio

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
    
    -- Thêm foreign key constraint cho staff_id nếu cần
    -- CONSTRAINT FK_support_chat_Account FOREIGN KEY (staff_id) REFERENCES Account(account_id)
    
    PRINT 'Đã tạo bảng support_chat thành công!';
END
ELSE
BEGIN
    PRINT 'Bảng support_chat đã tồn tại!';
    
    -- Kiểm tra và thêm các column còn thiếu nếu cần
    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'support_chat' AND COLUMN_NAME = 'sender_type')
    BEGIN
        ALTER TABLE support_chat ADD sender_type NVARCHAR(20) NOT NULL DEFAULT 'CUSTOMER';
        PRINT 'Đã thêm column sender_type';
    END
    
    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'support_chat' AND COLUMN_NAME = 'sender_name')
    BEGIN
        ALTER TABLE support_chat ADD sender_name NVARCHAR(100) NOT NULL;
        PRINT 'Đã thêm column sender_name';
    END
    
    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'support_chat' AND COLUMN_NAME = 'is_read')
    BEGIN
        ALTER TABLE support_chat ADD is_read BIT NOT NULL DEFAULT 0;
        PRINT 'Đã thêm column is_read';
    END
    
    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'support_chat' AND COLUMN_NAME = 'staff_id')
    BEGIN
        ALTER TABLE support_chat ADD staff_id INT NULL;
        PRINT 'Đã thêm column staff_id';
    END
END

-- Kiểm tra cấu trúc bảng
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'support_chat'
ORDER BY ORDINAL_POSITION;

PRINT 'Hoàn thành kiểm tra/tạo bảng support_chat!';

-- Thêm dữ liệu mẫu để test (optional)
-- IF NOT EXISTS (SELECT 1 FROM support_chat)
-- BEGIN
--     -- Giả sử có support_request với id = 1
--     INSERT INTO support_chat (support_request_id, sender_type, sender_name, message_content)
--     VALUES (1, 'CUSTOMER', 'Nguyễn Thu Lan', 'Tôi vào đăng nhập nhưng hệ thống báo sai mật khẩu dù nhập đúng.');
--     
--     INSERT INTO support_chat (support_request_id, sender_type, sender_name, message_content)
--     VALUES (1, 'STAFF', 'Thanh Xuân', 'Chào bạn, vui lòng kiểm tra lại Caps Lock và thử đăng nhập lại.');
--     
--     PRINT 'Đã thêm dữ liệu mẫu vào bảng support_chat!';
-- END
