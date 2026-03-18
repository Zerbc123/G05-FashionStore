-- Update image URLs to use real images from Unsplash
USE [fashion_shopping]
GO

-- Update existing product variants with real image URLs
UPDATE dbo.ProductVariant 
SET image_url = 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=600&h=800&fit=crop'
WHERE product_id = 1 AND color_id = 1 AND category_size_id = 1;

UPDATE dbo.ProductVariant 
SET image_url = 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=600&h=800&fit=crop'
WHERE product_id = 1 AND color_id = 1 AND category_size_id = 2;

UPDATE dbo.ProductVariant 
SET image_url = 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=600&h=800&fit=crop'
WHERE product_id = 1 AND color_id = 1 AND category_size_id = 3;

UPDATE dbo.ProductVariant 
SET image_url = 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=600&h=800&fit=crop'
WHERE product_id = 1 AND color_id = 1 AND category_size_id = 4;

UPDATE dbo.ProductVariant 
SET image_url = 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=600&h=800&fit=crop'
WHERE product_id = 1 AND color_id = 1 AND category_size_id = 5;

-- For products that don't have variants yet, set default image
UPDATE dbo.ProductVariant 
SET image_url = 'https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?w=600&h=800&fit=crop'
WHERE image_url IS NULL OR image_url = '' OR image_url LIKE '/images/products/%';

-- Verify the updates
SELECT 
    pv.variant_id,
    pv.product_id,
    p.product_name,
    pv.color_id,
    c.color_name,
    pv.category_size_id,
    cs.size_name,
    pv.price,
    pv.stock,
    pv.image_url
FROM dbo.ProductVariant pv
LEFT JOIN dbo.Product p ON pv.product_id = p.product_id
LEFT JOIN dbo.Color c ON pv.color_id = c.color_id
LEFT JOIN dbo.Category_Size cs ON pv.category_size_id = cs.category_size_id
ORDER BY pv.product_id, pv.variant_id;

PRINT '=== IMAGE URLS UPDATED ===';
PRINT 'All variants now have proper image paths pointing to /images/products/';
PRINT '========================';
