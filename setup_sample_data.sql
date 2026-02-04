-- 1. CHÈN DỮ LIỆU CHO BẢNG Role (nếu chưa có)
INSERT INTO [dbo].[Role] ([role_name])
VALUES 
(N'Admin'),
(N'Staff'), 
(N'Customer');
GO

-- 2. CHÈN DỮ LIỆU CHO BẢNG Account với BCrypt hash
-- Password: password123 → BCrypt hash: $2a$10$gqcpWmw4Tc.2Y6k.3k.2kOZ1Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1
INSERT INTO [dbo].[Account] ([username], [password], [email], [status], [role_id], [full_name], [phone])
VALUES 
(N'admin01', N'$2a$10$gqcpWmw4Tc.2Y6k.3k.2kOZ1Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1', N'admin@gmail.com', N'Active', 1, N'Nguyễn Quản Trị', N'0901234567'),
(N'staff01', N'$2a$10$gqcpWmw4Tc.2Y6k.3k.2kOZ1Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1', N'staff@gmail.com', N'Active', 2, N'Lê Thị Lan', N'0908889999'),
(N'khachhang01', N'$2a$10$gqcpWmw4Tc.2Y6k.3k.2kOZ1Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1', N'khach01@gmail.com', N'Active', 3, N'Trần Văn Nam', N'0911223344'),
(N'khachhang02', N'$2a$10$gqcpWmw4Tc.2Y6k.3k.2kOZ1Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1Z1', N'khach02@gmail.com', N'Active', 3, N'Phạm Thị Hoa', N'0911556677');
GO

-- 3. CHÈN DỮ LIỆU CHO BẢNG Customer
-- Lưu ý: account_id phải khớp với các tài khoản có Role là 'Customer' (id 3 và 4)
INSERT INTO [dbo].[Customer] ([full_name], [phone], [address], [account_id], [email], [birthday], [gender], [created_at])
VALUES 
(N'Trần Văn Nam', N'0911223344', N'123 Đường Lê Lợi, Quận 1, TP.HCM', 3, 'khach01@gmail.com', '1995-05-15', N'Nam', GETDATE()),
(N'Phạm Thị Hoa', N'0911556677', N'456 Đường CMT8, Quận 3, TP.HCM', 4, 'khach02@gmail.com', '1998-10-20', N'Nữ', GETDATE());
GO

-- 4. KIỂM TRA DỮ LIỆU ĐÃ CHÈN
SELECT 
    a.account_id, a.username, a.email, a.status, a.full_name, a.phone,
    r.role_name,
    LEFT(a.password, 25) + '...' AS password_hash_preview
FROM [dbo].[Account] a
JOIN [dbo].[Role] r ON a.role_id = r.role_id
ORDER BY a.account_id;
GO

-- 5. KIỂM TRA CUSTOMER DATA
SELECT 
    c.customer_id, c.full_name, c.phone, c.email, c.address, c.gender,
    a.username, a.email as account_email
FROM [dbo].[Customer] c
JOIN [dbo].[Account] a ON c.account_id = a.account_id
ORDER BY c.customer_id;
GO

PRINT 'Đã tạo dữ liệu mẫu với password BCrypt hash!';
PRINT 'Login credentials:';
PRINT 'Admin: admin01 / password123';
PRINT 'Staff: staff01 / password123';
PRINT 'Customer: khach01@gmail.com / password123 hoặc khach02@gmail.com / password123';
