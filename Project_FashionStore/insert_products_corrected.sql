-- Insert sample products into Product table
-- Based on actual database schema: product_id (int identity), product_name (nvarchar 150), description (nvarchar max), category_id (int), account_id (int)
-- SQL Server compatible

-- First, insert sample categories
INSERT INTO Category (category_name) VALUES ('Men''s Fashion'), ('Women''s Fashion'), ('Accessories'), ('Shoes');

-- First, insert roles (required for Account foreign key)
INSERT INTO Role (role_name) VALUES ('Admin'), ('User');

-- First, insert sample account (admin)
INSERT INTO Account (username, [password], email, [status], role_id, full_name, phone) 
VALUES ('admin', 'admin123', 'admin@fashionstore.com', 'active', 1, 'Administrator', '0123456789');

-- Get the inserted account_id
DECLARE @admin_account_id INT = SCOPE_IDENTITY();

-- Now insert products using the actual account_id
INSERT INTO Product (product_name, description, category_id, account_id) VALUES
('Classic White Shirt', 'Classic cotton white shirt for formal occasions', 1, @admin_account_id);
('Navy Blue Suit', 'Professional navy blue business suit', 1, 1),
('Gray Wool Sweater', 'Warm wool sweater for winter', 1, 1),
('Polo Shirt Red', 'Casual cotton polo shirt', 1, 1),
('Business Tie Blue', 'Silk business tie', 1, 1),
('Casual T-Shirt', 'Basic cotton t-shirt', 1, 1),
('Winter Coat', 'Heavy winter coat with hood', 1, 1),
('Denim Jeans', 'Classic straight-fit denim jeans', 1, 1),
('Sports Shorts', 'Athletic performance shorts', 1, 1),
('Leather Belt Brown', 'Genuine leather belt', 1, 1),
('Wallet Leather', 'Genuine leather wallet', 1, 1),
('Watch Silver', 'Elegant silver watch', 1, 1),

-- Women's Fashion (12 items)
('Floral Summer Dress', 'Light floral print summer dress', 2, 1),
('Business Blazer', 'Professional office blazer', 2, 1),
('Silk Blouse Pink', 'Elegant silk blouse', 2, 1),
('Yoga Pants Black', 'Comfortable stretch yoga pants', 2, 1),
('Cardigan Sweater', 'Cozy knit cardigan', 2, 1),
('Maxi Dress Blue', 'Elegant floor-length maxi dress', 2, 1),
('Tank Top White', 'Basic cotton tank top', 2, 1),
('Skinny Jeans', 'Modern skinny fit jeans', 2, 1),
('Scarf Wool', 'Warm winter wool scarf', 2, 1),
('Handbag Beige', 'Leather designer handbag', 2, 1),
('Necklace Gold', 'Gold plated necklace', 2, 1),
('Earrings Diamond', 'Diamond stud earrings', 2, 1),

-- Accessories (12 items)
('Sunglasses Aviator', 'Classic aviator sunglasses', 3, 1),
('Bracelet Leather', 'Braided leather bracelet', 3, 1),
('Ring Silver', 'Sterling silver ring', 3, 1),
('Scarf Silk', 'Luxury silk scarf', 3, 1),
('Hat Fedora', 'Classic fedora hat', 3, 1),
('Gloves Leather', 'Black leather gloves', 3, 1),
('Phone Case', 'Protective phone case', 3, 1),
('Backpack School', 'Colorful school backpack', 3, 1),
('Baseball Cap', 'Adjustable baseball cap', 3, 1),
('Pajamas Set', 'Comfortable pajama set', 3, 1),
('Party Dress', 'Beautiful party dress', 3, 1),
('Cartoon T-Shirt', 'Fun cartoon print t-shirt', 3, 1),

-- Shoes (12 items)
('Dress Shoes Black', 'Formal leather dress shoes', 4, 1),
('Sneakers White', 'Casual canvas sneakers', 4, 1),
('High Heels Red', 'Stiletto high heels', 4, 1),
('Pumps Nude', 'Classic nude pumps', 4, 1),
('Black Leather Jacket', 'Genuine leather motorcycle jacket', 4, 1),
('School Uniform', 'Standard school uniform set', 4, 1),
('Sneakers Light-Up', 'Light-up sneakers for kids', 4, 1),
('Winter Jacket', 'Warm winter jacket', 4, 1),
('Shorts Denim', 'Casual denim shorts', 4, 1),
('Sandals Summer', 'Summer beach sandals', 4, 1),
('Sweater Hoodie', 'Cozy hoodie sweater', 4, 1),
('Rain Boots', 'Waterproof rain boots', 4, 1);
