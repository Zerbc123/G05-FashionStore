-- Test database connection and check if products exist
USE fashion_shopping;
GO

-- Check if tables exist
SELECT TABLE_NAME 
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_TYPE = 'BASE TABLE' 
AND TABLE_NAME IN ('Product', 'products', 'Category', 'Account');

-- Check product data
SELECT COUNT(*) as total_products FROM Product;
SELECT COUNT(*) as total_products_simple FROM products;

-- View sample products
SELECT TOP 10 p.product_id, p.product_name, p.description, c.category_name
FROM Product p
LEFT JOIN Category c ON p.category_id = c.category_id;

-- Check if there are any products in the simple products table
SELECT TOP 10 * FROM products;