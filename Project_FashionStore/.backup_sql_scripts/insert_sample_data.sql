-- Insert sample data for testing
USE [fashion_shopping]
GO

-- Insert more products
INSERT INTO dbo.Product (product_name, description, category_id, account_id) VALUES
(N'Áo Thun Trắng', N'Áo thun trắng chất liệu cotton cao cấp', 1, 1),
(N'Áo Thun Đen', N'Áo thun đen thời trang sang trọng', 1, 1),
(N'Quần Jean Nam', N'Quần jean nam form slim fit', 1, 1),
(N'Váy Len', N'Váy len len lông mềm mại', 1, 1);

-- Insert more colors
INSERT INTO dbo.Color (color_name) VALUES
(N'Trắng'),
(N'Xanh Dương Biển'),
(N'Xanh Lá'),
(N'Đỏ'),
(N'Hồng'),
(N'Vàng');

-- Insert more sizes
INSERT INTO dbo.Category_Size (size_name, category_id) VALUES
(N'S', 1),
(N'M', 1),
(N'L', 1),
(N'XL', 1),
(N'XXL', 1);

-- Insert product variants
INSERT INTO dbo.ProductVariant (product_id, color_id, category_size_id, price, stock, image_url) VALUES
-- Áo Thun Trắng (product_id = 1)
(1, 1, 1, 150000.00, 50, '/images/white_shirt_s.jpg'),
(1, 1, 2, 150000.00, 50, '/images/white_shirt_m.jpg'),
(1, 1, 3, 150000.00, 50, '/images/white_shirt_l.jpg'),
(1, 1, 4, 150000.00, 30, '/images/white_shirt_xl.jpg'),
(1, 1, 5, 150000.00, 20, '/images/white_shirt_xxl.jpg'),

-- Áo Thun Đen (product_id = 2)
(2, 2, 1, 150000.00, 40, '/images/black_shirt_s.jpg'),
(2, 2, 2, 150000.00, 40, '/images/black_shirt_m.jpg'),
(2, 2, 3, 150000.00, 40, '/images/black_shirt_l.jpg'),
(2, 2, 4, 150000.00, 25, '/images/black_shirt_xl.jpg'),
(2, 2, 5, 150000.00, 15, '/images/black_shirt_xxl.jpg'),

-- Quần Jean Nam (product_id = 3)
(3, 1, 2, 350000.00, 30, '/images/jeans_s.jpg'),
(3, 1, 3, 350000.00, 30, '/images/jeans_m.jpg'),
(3, 1, 4, 350000.00, 30, '/images/jeans_l.jpg'),
(3, 1, 5, 350000.00, 20, '/images/jeans_xl.jpg'),

-- Váy Len (product_id = 4)
(4, 3, 1, 450000.00, 25, '/images/sweater_s.jpg'),
(4, 3, 2, 450000.00, 25, '/images/sweater_m.jpg'),
(4, 3, 3, 450000.00, 25, '/images/sweater_l.jpg'),
(4, 3, 4, 450000.00, 25, '/images/sweater_xl.jpg'),
(4, 4, 1, 450000.00, 20, '/images/sweater_s_blue.jpg'),
(4, 5, 1, 450000.00, 20, '/images/sweater_m_blue.jpg'),
(4, 6, 1, 450000.00, 20, '/images/sweater_l_blue.jpg'),
(4, 7, 1, 450000.00, 20, '/images/sweater_xl_blue.jpg');

-- Áo Thun Trắng (product_id = 5)
(5, 1, 1, 150000.00, 60, '/images/white_shirt_s.jpg'),
(5, 1, 2, 150000.00, 60, '/images/white_shirt_m.jpg'),
(5, 1, 3, 150000.00, 60, '/images/white_shirt_l.jpg'),
(5, 1, 4, 150000.00, 60, '/images/white_shirt_xl.jpg'),
(5, 1, 5, 150000.00, 60, '/images/white_shirt_xxl.jpg'),

-- Áo Thun Đen (product_id = 5)
(5, 2, 1, 150000.00, 50, '/images/black_shirt_s.jpg'),
(5, 2, 2, 150000.00, 50, '/images/black_shirt_m.jpg'),
(5, 2, 3, 150000.00, 50, '/images/black_shirt_l.jpg'),
(5, 2, 4, 150000.00, 50, '/images/black_shirt_xl.jpg'),
(5, 2, 5, 150000.00, 50, '/images/black_shirt_xxl.jpg');

PRINT 'Sample data inserted successfully!';
PRINT 'Total products: ' + (SELECT COUNT(*) FROM dbo.Product);
PRINT 'Total variants: ' + (SELECT COUNT(*) FROM dbo.ProductVariant);
