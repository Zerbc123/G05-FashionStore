-- Xóa admin/staff cũ nếu có
DELETE FROM Account WHERE username IN ('admin', 'staff');

-- Thêm admin account với password đã hash (password: 123456)
INSERT INTO Account (username, password, email, status, role_id, full_name, phone)
VALUES 
('admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 
 'admin@gmail.com', 'active', 
 (SELECT role_id FROM Role WHERE role_name = 'Admin'), 
 'Admin User', '0900000000');

-- Thêm staff account với password đã hash (password: 123456) 
INSERT INTO Account (username, password, email, status, role_id, full_name, phone)
VALUES 
('staff', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
 'staff@gmail.com', 'active',
 (SELECT role_id FROM Role WHERE role_name = 'Staff'),
 'Staff User', '0911111111');

-- Kiểm tra accounts đã tạo
SELECT a.account_id, a.username, a.email, a.status, r.role_name, a.full_name, a.phone,
       LEFT(a.password, 20) + '...' AS password_preview
FROM Account a
JOIN Role r ON a.role_id = r.role_id
WHERE a.username IN ('admin', 'staff');

-- Thông báo
PRINT 'Đã tạo admin/staff với password BCrypt hash';
PRINT 'Login với: admin@gmail.com / 123456 hoặc staff@gmail.com / 123456';
