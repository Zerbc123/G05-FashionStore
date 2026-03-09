-- Script để tạo bảng order_items
-- Chạy script này trong SQL Server Management Studio nếu bạn muốn lưu chi tiết sản phẩm

-- Kiểm tra xem bảng đã tồn tại chưa
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'order_items')
BEGIN
    -- Tạo bảng order_items
    CREATE TABLE order_items (
        order_item_id INT IDENTITY(1,1) PRIMARY KEY,
        order_id INT NOT NULL,
        variant_id INT NOT NULL,
        quantity INT NOT NULL,
        unit_price FLOAT NOT NULL,
        total_price FLOAT NOT NULL,
        
        -- Foreign key constraints
        CONSTRAINT FK_order_items_orders FOREIGN KEY (order_id) REFERENCES orders(order_id),
        CONSTRAINT FK_order_items_productvariant FOREIGN KEY (variant_id) REFERENCES ProductVariant(variant_id)
    );
    
    PRINT 'Đã tạo bảng order_items thành công!';
END
ELSE
BEGIN
    PRINT 'Bảng order_items đã tồn tại!';
END

-- Kiểm tra cấu trúc bảng sau khi tạo
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'order_items'
ORDER BY ORDINAL_POSITION;
