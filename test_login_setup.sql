-- Kiểm tra xem admin/staff đã được tạo chưa
SELECT a.account_id, a.username, a.email, a.status, a.password, r.role_name
FROM Account a
JOIN Role r ON a.role_id = r.role_id
WHERE a.username IN ('admin', 'staff');

-- Kiểm tra Role data
SELECT * FROM Role;

-- Nếu chưa có data, thêm lại
INSERT INTO Role (role_name) VALUES 
('Admin'),
('Staff'), 
('Customer')
WHERE NOT EXISTS (SELECT 1 FROM Role WHERE role_name IN ('Admin', 'Staff', 'Customer'));

-- Thêm admin với password plain text trước để test
INSERT INTO Account (username, password, email, status, role_id, full_name, phone)
VALUES 
('admin', '123456', 'admin@gmail.com', 'active', 
 (SELECT role_id FROM Role WHERE role_name = 'Admin'), 
 'Admin User', '0900000000')
WHERE NOT EXISTS (SELECT 1 FROM Account WHERE username = 'admin');

-- Thêm staff với password plain text trước để test
INSERT INTO Account (username, password, email, status, role_id, full_name, phone)
VALUES 
('staff', '123456', 'staff@gmail.com', 'active',
 (SELECT role_id FROM Role WHERE role_name = 'Staff'),
 'Staff User', '0911111111')
WHERE NOT EXISTS (SELECT 1 FROM Account WHERE username = 'staff');
