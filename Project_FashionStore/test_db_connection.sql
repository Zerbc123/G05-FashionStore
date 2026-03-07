-- Test kết nối database và kiểm tra account admin
USE fashion_shopping;
GO

-- Kiểm tra account admin
SELECT 
    a.account_id,
    a.email,
    a.password,
    a.full_name,
    a.status,
    r.role_name
FROM dbo.Account a
JOIN dbo.Role r ON a.role_id = r.role_id
WHERE a.email = 'duypro2004@gmail.com';

-- Kiểm tra products
SELECT COUNT(*) as total_products FROM Product;

-- Kiểm tra variants
SELECT COUNT(*) as total_variants FROM ProductVariant;

-- Test BCrypt hash verification
DECLARE @inputPassword NVARCHAR(100) = '123456';
DECLARE @storedHash NVARCHAR(100) = '$2a$10$7sF9uZ6C8WwQ7R1pZK9V0eQxF7p7JkK9kXQ1m9nB5yW6b3zYQpG8K';

-- SQL Server không có BCrypt builtin, chỉ để tham khảo
SELECT 'Password to test: ' + @inputPassword;
SELECT 'Stored hash: ' + @storedHash;