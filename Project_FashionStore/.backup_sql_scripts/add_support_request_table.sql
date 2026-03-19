-- Script để thêm bảng support_request vào database fashion_store hiện có
-- Chạy script này trong SQL Server Management Studio

USE fashion_store;
GO

-- Kiểm tra xem bảng support_request đã tồn tại chưa
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'support_request')
BEGIN
    -- Tạo bảng support_request
    CREATE TABLE support_request (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        title NVARCHAR(255) NOT NULL,
        description NVARCHAR(MAX) NULL,
        customer_name NVARCHAR(100) NOT NULL,
        customer_email NVARCHAR(100) NULL,
        status NVARCHAR(20) NOT NULL DEFAULT 'OPEN',
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NULL,
        assigned_staff_id INT NULL,
        assigned_staff_name NVARCHAR(100) NULL
    );
    
    -- Thêm indexes cho performance
    CREATE INDEX IX_support_request_status ON support_request(status);
    CREATE INDEX IX_support_request_customer_email ON support_request(customer_email);
    CREATE INDEX IX_support_request_assigned_staff_id ON support_request(assigned_staff_id);
    CREATE INDEX IX_support_request_created_at ON support_request(created_at);
    
    -- Thêm foreign key constraint cho assigned_staff_id nếu cần
    -- CONSTRAINT FK_support_request_Account FOREIGN KEY (assigned_staff_id) REFERENCES Account(account_id)
    
    PRINT 'Đã tạo bảng support_request thành công!';
END
ELSE
BEGIN
    PRINT 'Bảng support_request đã tồn tại!';
    
    -- Kiểm tra và thêm các column còn thiếu nếu cần
    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'support_request' AND COLUMN_NAME = 'assigned_staff_id')
    BEGIN
        ALTER TABLE support_request ADD assigned_staff_id INT NULL;
        PRINT 'Đã thêm column assigned_staff_id';
    END
    
    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'support_request' AND COLUMN_NAME = 'assigned_staff_name')
    BEGIN
        ALTER TABLE support_request ADD assigned_staff_name NVARCHAR(100) NULL;
        PRINT 'Đã thêm column assigned_staff_name';
    END
    
    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'support_request' AND COLUMN_NAME = 'updated_at')
    BEGIN
        ALTER TABLE support_request ADD updated_at DATETIME2 NULL;
        PRINT 'Đã thêm column updated_at';
    END
END

-- Kiểm tra cấu trúc bảng
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'support_request'
ORDER BY ORDINAL_POSITION;

PRINT 'Hoàn thành kiểm tra/tạo bảng support_request!';
