-- Script để thêm các column còn thiếu vào bảng orders
-- Chạy script này trong SQL Server Management Studio

-- Kiểm tra và thêm các column nếu chưa tồn tại
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'orders' AND COLUMN_NAME = 'order_code')
BEGIN
    ALTER TABLE orders ADD order_code NVARCHAR(50) NOT NULL DEFAULT '';
END

IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'orders' AND COLUMN_NAME = 'delivery_address')
BEGIN
    ALTER TABLE orders ADD delivery_address NVARCHAR(500) NOT NULL DEFAULT '';
END

IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'orders' AND COLUMN_NAME = 'confirmed_by')
BEGIN
    ALTER TABLE orders ADD confirmed_by NVARCHAR(100) NULL;
END

IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'orders' AND COLUMN_NAME = 'confirmed_date')
BEGIN
    ALTER TABLE orders ADD confirmed_date DATETIME NULL;
END

IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'orders' AND COLUMN_NAME = 'cancelled_by')
BEGIN
    ALTER TABLE orders ADD cancelled_by NVARCHAR(100) NULL;
END

IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'orders' AND COLUMN_NAME = 'cancelled_date')
BEGIN
    ALTER TABLE orders ADD cancelled_date DATETIME NULL;
END

IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'orders' AND COLUMN_NAME = 'cancellation_reason')
BEGIN
    ALTER TABLE orders ADD cancellation_reason NVARCHAR(500) NULL;
END

-- Thêm unique constraint cho order_code nếu chưa có
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'UQ_orders_order_code' AND object_id = OBJECT_ID('orders'))
BEGIN
    ALTER TABLE orders ADD CONSTRAINT UQ_orders_order_code UNIQUE (order_code);
END

-- In ra thông báo hoàn thành
PRINT 'Đã thêm các column còn thiếu vào bảng orders thành công!';
