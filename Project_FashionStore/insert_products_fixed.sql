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
-- Men's Fashion (12 items)
('Classic White Shirt', 'Classic cotton white shirt for formal occasions', 1, @admin_account_id),
('Navy Blue Suit', 'Professional navy blue business suit', 1, @admin_account_id),
('Gray Wool Sweater', 'Warm wool sweater for winter', 1, @admin_account_id),
('Polo Shirt Red', 'Casual cotton polo shirt', 1, @admin_account_id),
('Business Tie Blue', 'Silk business tie', 1, @admin_account_id),
('Casual T-Shirt', 'Basic cotton t-shirt', 1, @admin_account_id),
('Winter Coat', 'Heavy winter coat with hood', 1, @admin_account_id),
('Denim Jeans', 'Classic straight-fit denim jeans', 1, @admin_account_id),
('Sports Shorts', 'Athletic performance shorts', 1, @admin_account_id),
('Leather Belt Brown', 'Genuine leather belt', 1, @admin_account_id),
('Wallet Leather', 'Genuine leather wallet', 1, @admin_account_id),
('Watch Silver', 'Elegant silver watch', 1, @admin_account_id),

-- Women's Fashion (12 items)
('Floral Summer Dress', 'Light floral print summer dress', 2, @admin_account_id),
('Business Blazer', 'Professional office blazer', 2, @admin_account_id),
('Silk Blouse Pink', 'Elegant silk blouse', 2, @admin_account_id),
('Yoga Pants Black', 'Comfortable stretch yoga pants', 2, @admin_account_id),
('Cardigan Sweater', 'Cozy knit cardigan', 2, @admin_account_id),
('Maxi Dress Blue', 'Elegant floor-length maxi dress', 2, @admin_account_id),
('Tank Top White', 'Basic cotton tank top', 2, @admin_account_id),
('Skinny Jeans', 'Modern skinny fit jeans', 2, @admin_account_id),
('Scarf Wool', 'Warm winter wool scarf', 2, @admin_account_id),
('Handbag Beige', 'Leather designer handbag', 2, @admin_account_id),
('Necklace Gold', 'Gold plated necklace', 2, @admin_account_id),
('Earrings Diamond', 'Diamond stud earrings', 2, @admin_account_id),

-- Accessories (12 items)
('Sunglasses Aviator', 'Classic aviator sunglasses', 3, @admin_account_id),
('Bracelet Leather', 'Braided leather bracelet', 3, @admin_account_id),
('Ring Silver', 'Sterling silver ring', 3, @admin_account_id),
('Scarf Silk', 'Luxury silk scarf', 3, @admin_account_id),
('Hat Fedora', 'Classic fedora hat', 3, @admin_account_id),
('Gloves Leather', 'Black leather gloves', 3, @admin_account_id),
('Phone Case', 'Protective phone case', 3, @admin_account_id),
('Backpack School', 'Colorful school backpack', 3, @admin_account_id),
('Baseball Cap', 'Adjustable baseball cap', 3, @admin_account_id),
('Pajamas Set', 'Comfortable pajama set', 3, @admin_account_id),
('Party Dress', 'Beautiful party dress', 3, @admin_account_id),
('Cartoon T-Shirt', 'Fun cartoon print t-shirt', 3, @admin_account_id),

-- Shoes (12 items)
('Dress Shoes Black', 'Formal leather dress shoes', 4, @admin_account_id),
('Sneakers White', 'Casual canvas sneakers', 4, @admin_account_id),
('High Heels Red', 'Stiletto high heels', 4, @admin_account_id),
('Pumps Nude', 'Classic nude pumps', 4, @admin_account_id),
('Black Leather Jacket', 'Genuine leather motorcycle jacket', 4, @admin_account_id),
('School Uniform', 'Standard school uniform set', 4, @admin_account_id),
('Sneakers Light-Up', 'Light-up sneakers for kids', 4, @admin_account_id),
('Winter Jacket', 'Warm winter jacket', 4, @admin_account_id),
('Shorts Denim', 'Casual denim shorts', 4, @admin_account_id),
('Sandals Summer', 'Summer beach sandals', 4, @admin_account_id),
('Sweater Hoodie', 'Cozy hoodie sweater', 4, @admin_account_id),
('Rain Boots', 'Waterproof rain boots', 4, @admin_account_id);
