-- Script kiểm tra và sửa database để khớp với entities
-- Chạy trong SQL Server Management Studio với database fashion_store

USE fashion_store;
GO

PRINT 'Bắt đầu kiểm tra và sửa database...';

-- 1. Kiểm tra và tạo bảng support_request
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'support_request')
BEGIN
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
    
    CREATE INDEX IX_support_request_status ON support_request(status);
    CREATE INDEX IX_support_request_customer_email ON support_request(customer_email);
    CREATE INDEX IX_support_request_assigned_staff_id ON support_request(assigned_staff_id);
    
    PRINT '✅ Đã tạo bảng support_request';
END
ELSE
BEGIN
    PRINT '✅ Bảng support_request đã tồn tại';
END

-- 2. Kiểm tra các bảng chính
DECLARE @table_count INT;
SELECT @table_count = COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE';

PRINT 'Tổng số bảng hiện tại: ' + CAST(@table_count AS NVARCHAR(10));

-- 3. Hiển thị danh sách các bảng
SELECT 
    TABLE_NAME,
    CASE 
        WHEN TABLE_NAME IN ('Account', 'Role', 'Customer', 'Product', 'Category', 'orders', 'support_request') 
        THEN '✅ Core table'
        ELSE '📋 Other table'
    END as Status
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_TYPE = 'BASE TABLE'
ORDER BY 
    CASE 
        WHEN TABLE_NAME IN ('Account', 'Role', 'Customer', 'Product', 'Category', 'orders', 'support_request') THEN 0
        ELSE 1
    END,
    TABLE_NAME;

-- 4. Kiểm tra foreign key constraints
SELECT 
    fk.name AS ForeignKeyName,
    tp.name AS ParentTable,
    cp.name AS ChildTable,
    cc.name AS ColumnName
FROM sys.foreign_keys fk
INNER JOIN sys.tables tp ON fk.parent_object_id = tp.object_id
INNER JOIN sys.tables cp ON fk.referenced_object_id = cp.object_id
INNER JOIN sys.foreign_key_columns fkc ON fk.object_id = fkc.constraint_object_id
INNER JOIN sys.columns cc ON fkc.parent_object_id = cc.object_id AND fkc.parent_column_id = cc.column_id
WHERE tp.name IN ('Account', 'Role', 'Customer', 'Product', 'Category', 'orders', 'support_request')
ORDER BY tp.name, fk.name;

PRINT '✅ Hoàn thành kiểm tra database!';
PRINT '';
PRINT 'Các bước tiếp theo:';
PRINT '1. Chạy ứng dụng Spring Boot';
PRINT '2. Kiểm tra log để xem có lỗi mapping nào không';
PRINT '3. Nếu có lỗi, kiểm tra lại column names và data types';
