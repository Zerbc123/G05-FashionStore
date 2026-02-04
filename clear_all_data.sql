-- Xóa dữ liệu theo thứ tự đúng để tránh foreign key conflict
-- 1. Xóa Customer trước (vì Customer tham chiếu đến Account)
DELETE FROM Customer;

-- 2. Xóa Account sau khi không còn Customer tham chiếu
DELETE FROM Account;

-- Reset identity về 1 (tùy chọn)
DBCC CHECKIDENT ('Account', RESEED, 1);
DBCC CHECKIDENT ('Customer', RESEED, 1);

-- Kiểm tra kết quả
SELECT 'Account rows: ' + CAST(COUNT(*) AS VARCHAR) FROM Account;
SELECT 'Customer rows: ' + CAST(COUNT(*) AS VARCHAR) FROM Customer;

-- Thông báo xóa thành công
PRINT 'Đã xóa hết dữ liệu trong bảng Account và Customer';
