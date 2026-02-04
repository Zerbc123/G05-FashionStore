-- Xóa hết dữ liệu trong bảng Account
DELETE FROM Account;

-- Reset identity về 1 (tùy chọn)
DBCC CHECKIDENT ('Account', RESEED, 1);

-- Kiểm tra kết quả
SELECT * FROM Account;

-- Thông báo xóa thành công
PRINT 'Đã xóa hết dữ liệu trong bảng Account';
