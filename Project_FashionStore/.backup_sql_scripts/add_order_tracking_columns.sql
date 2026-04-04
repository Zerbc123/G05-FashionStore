-- Thêm các column để tracking trạng thái order (SQL SERVER)
-- Chạy script này để hỗ trợ tính năng auto-complete order sau 1 phút

ALTER TABLE orders 
ADD confirmed_by NVARCHAR(100),
    confirmed_date DATETIME2,
    cancelled_by NVARCHAR(100),
    cancelled_date DATETIME2,
    cancellation_reason NVARCHAR(500);

-- Update confirmed_date cho các order đã CONFIRMED (set = order_date để tạm thời)
UPDATE orders 
SET confirmed_date = CAST(order_date AS DATETIME2)
WHERE status = 'CONFIRMED' AND confirmed_date IS NULL;

-- Kiểm tra kết quả
SELECT TOP 10 order_id, status, order_date, confirmed_date, cancelled_date 
FROM orders 
ORDER BY order_id DESC;
