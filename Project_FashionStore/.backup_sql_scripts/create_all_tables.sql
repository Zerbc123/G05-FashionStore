-- Script để tạo tất cả các bảng cần thiết cho Fashion Store
-- Chạy script này trong SQL Server Management Studio với database fashion_shopping

USE fashion_shopping;
GO

-- Tạo bảng Role
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Role')
BEGIN
    CREATE TABLE Role (
        role_id INT IDENTITY(1,1) PRIMARY KEY,
        role_name NVARCHAR(50) NOT NULL UNIQUE,
        description NVARCHAR(255) NULL
    );
    
    INSERT INTO Role (role_name, description) VALUES 
        ('ADMIN', 'Quản trị viên hệ thống'),
        ('STAFF', 'Nhân viên hỗ trợ'),
        ('SUPPORT', 'Nhân viên hỗ trợ khách hàng'),
        ('Customer', 'Khách hàng');
    
    PRINT 'Đã tạo bảng Role thành công!';
END

-- Tạo bảng Account
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Account')
BEGIN
    CREATE TABLE Account (
        account_id INT IDENTITY(1,1) PRIMARY KEY,
        username NVARCHAR(50) NOT NULL UNIQUE,
        password NVARCHAR(255) NULL,
        email NVARCHAR(100) NOT NULL UNIQUE,
        full_name NVARCHAR(100) NOT NULL,
        phone NVARCHAR(20) NOT NULL,
        status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
        role_id INT NULL,
        
        CONSTRAINT FK_Account_Role FOREIGN KEY (role_id) REFERENCES Role(role_id)
    );
    
    CREATE INDEX IX_Account_username ON Account(username);
    CREATE INDEX IX_Account_email ON Account(email);
    CREATE INDEX IX_Account_status ON Account(status);
    
    PRINT 'Đã tạo bảng Account thành công!';
END

-- Tạo bảng Customer
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Customer')
BEGIN
    CREATE TABLE Customer (
        customer_id INT IDENTITY(1,1) PRIMARY KEY,
        account_id INT NOT NULL,
        full_name NVARCHAR(100) NOT NULL,
        email NVARCHAR(100) NOT NULL,
        phone NVARCHAR(20) NOT NULL,
        address NVARCHAR(500) NULL,
        gender BIT NULL,
        date_of_birth DATE NULL,
        created_date DATE NOT NULL DEFAULT GETDATE(),
        
        CONSTRAINT FK_Customer_Account FOREIGN KEY (account_id) REFERENCES Account(account_id)
    );
    
    PRINT 'Đã tạo bảng Customer thành công!';
END

-- Tạo bảng Category
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Category')
BEGIN
    CREATE TABLE Category (
        category_id INT IDENTITY(1,1) PRIMARY KEY,
        category_name NVARCHAR(100) NOT NULL,
        description NVARCHAR(500) NULL,
        status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
    );
    
    PRINT 'Đã tạo bảng Category thành công!';
END

-- Tạo bảng Product
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Product')
BEGIN
    CREATE TABLE Product (
        product_id INT IDENTITY(1,1) PRIMARY KEY,
        category_id INT NOT NULL,
        product_name NVARCHAR(200) NOT NULL,
        description NVARCHAR(MAX) NULL,
        price DECIMAL(10,2) NOT NULL,
        image_url NVARCHAR(500) NULL,
        status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
        created_date DATE NOT NULL DEFAULT GETDATE(),
        
        CONSTRAINT FK_Product_Category FOREIGN KEY (category_id) REFERENCES Category(category_id)
    );
    
    PRINT 'Đã tạo bảng Product thành công!';
END

-- Tạo bảng SupportRequest
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
    
    PRINT 'Đã tạo bảng support_request thành công!';
END

PRINT 'Hoàn thành tạo tất cả các bảng!';

-- Hiển thị danh sách các bảng đã tạo
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE' ORDER BY TABLE_NAME;
