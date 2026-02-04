-- Thêm dữ liệu vào bảng Role
INSERT INTO Role (role_name) VALUES 
('Admin'),
('Staff'), 
('Customer');

-- Kiểm tra Role data
SELECT * FROM Role;

-- Thêm admin account (password: 123456)
INSERT INTO Account (username, password, email, status, role_id, full_name, phone)
VALUES 
('admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 
 'admin@gmail.com', 'active', 
 (SELECT role_id FROM Role WHERE role_name = 'Admin'), 
 'Admin User', '0900000000');

-- Thêm staff account (password: 123456) 
INSERT INTO Account (username, password, email, status, role_id, full_name, phone)
VALUES 
('staff', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
 'staff@gmail.com', 'active',
 (SELECT role_id FROM Role WHERE role_name = 'Staff'),
 'Staff User', '0911111111');

-- Kiểm tra accounts đã tạo
SELECT a.account_id, a.username, a.email, a.status, r.role_name, a.full_name, a.phone
FROM Account a
JOIN Role r ON a.role_id = r.role_id
WHERE a.username IN ('admin', 'staff');

-- Test password hash verification (chạy để kiểm tra)
-- Username: admin, Password: 123456
-- Username: staff, Password: 123456
