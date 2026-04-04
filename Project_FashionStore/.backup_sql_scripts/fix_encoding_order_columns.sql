-- Fix encoding cho các cột tiếng Việt trong bảng orders
-- Xóa và tạo lại với NVARCHAR (dữ liệu cũ sẽ mất)

-- Xóa các cột cũ
ALTER TABLE orders 
DROP COLUMN confirmed_by, cancelled_by, cancellation_reason;

-- Thêm lại với NVARCHAR để hỗ trợ Unicode
ALTER TABLE orders 
ADD confirmed_by NVARCHAR(100),
    cancelled_by NVARCHAR(100),
    cancellation_reason NVARCHAR(500);

-- Kiểm tra
SELECT TOP 5 order_id, status, confirmed_by, cancelled_by, cancellation_reason
FROM orders 
ORDER BY order_id DESC;
