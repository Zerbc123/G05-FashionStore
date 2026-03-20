-- Script để tạo bảng Account trong database fashion_shopping
-- Chạy script này trong SQL Server Management Studio

-- Kiểm tra xem bảng đã tồn tại chưa
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'Account')
BEGIN
    -- Tạo bảng Account
    CREATE TABLE Account (
        account_id INT IDENTITY(1,1) PRIMARY KEY,
        username NVARCHAR(50) NOT NULL UNIQUE,
        password NVARCHAR(255) NULL,
        email NVARCHAR(100) NOT NULL UNIQUE,
        full_name NVARCHAR(100) NOT NULL,
        phone NVARCHAR(20) NOT NULL,
        status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
        role_id INT NULL,
        
        -- Foreign key constraint
        CONSTRAINT FK_Account_Role FOREIGN KEY (role_id) REFERENCES Role(role_id)
    );
    
    -- Thêm indexes cho performance
    CREATE INDEX IX_Account_username ON Account(username);
    CREATE INDEX IX_Account_email ON Account(email);
    CREATE INDEX IX_Account_status ON Account(status);
    CREATE INDEX IX_Account_role_id ON Account(role_id);
    
    PRINT 'Đã tạo bảng Account thành công!';
END
ELSE
BEGIN
    PRINT 'Bảng Account đã tồn tại!';
END

-- Kiểm tra cấu trúc bảng sau khi tạo
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'Account'
ORDER BY ORDINAL_POSITION;
