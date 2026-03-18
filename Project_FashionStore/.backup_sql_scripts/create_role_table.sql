-- Script để tạo bảng Role trong database fashion_shopping
-- Chạy script này trong SQL Server Management Studio

-- Kiểm tra xem bảng đã tồn tại chưa
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Role')
BEGIN
    -- Tạo bảng Role
    CREATE TABLE Role (
        role_id INT IDENTITY(1,1) PRIMARY KEY,
        role_name NVARCHAR(50) NOT NULL UNIQUE,
        description NVARCHAR(255) NULL
    );
    
    -- Insert các role mặc định
    INSERT INTO Role (role_name, description) VALUES 
        ('ADMIN', 'Quản trị viên hệ thống'),
        ('STAFF', 'Nhân viên hỗ trợ'),
        ('SUPPORT', 'Nhân viên hỗ trợ khách hàng'),
        ('Customer', 'Khách hàng');
    
    PRINT 'Đã tạo bảng Role và thêm dữ liệu mặc định thành công!';
END
ELSE
BEGIN
    PRINT 'Bảng Role đã tồn tại!';
    
    -- Kiểm tra và thêm các role mặc định nếu chưa có
    IF NOT EXISTS (SELECT * FROM Role WHERE role_name = 'ADMIN')
        INSERT INTO Role (role_name, description) VALUES ('ADMIN', 'Quản trị viên hệ thống');
    
    IF NOT EXISTS (SELECT * FROM Role WHERE role_name = 'STAFF')
        INSERT INTO Role (role_name, description) VALUES ('STAFF', 'Nhân viên hỗ trợ');
    
    IF NOT EXISTS (SELECT * FROM Role WHERE role_name = 'SUPPORT')
        INSERT INTO Role (role_name, description) VALUES ('SUPPORT', 'Nhân viên hỗ trợ khách hàng');
    
    IF NOT EXISTS (SELECT * FROM Role WHERE role_name = 'Customer')
        INSERT INTO Role (role_name, description) VALUES ('Customer', 'Khách hàng');
        
    PRINT 'Đã kiểm tra và thêm các role mặc định!';
END

-- Kiểm tra cấu trúc và dữ liệu bảng
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'Role'
ORDER BY ORDINAL_POSITION;

SELECT * FROM Role;
