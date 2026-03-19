-- Check current products and variants
USE [fashion_shopping]
GO

-- Check all products
SELECT 
    p.product_id,
    p.product_name,
    p.description,
    p.category_id,
    cat.category_name
FROM dbo.Product p
LEFT JOIN dbo.Category cat ON p.category_id = cat.category_id
ORDER BY p.product_id;

-- Check all variants with details
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

-- Check available colors and sizes
SELECT DISTINCT 
    c.color_id,
    c.color_name
FROM dbo.Color c
WHERE c.color_id IN (SELECT DISTINCT color_id FROM dbo.ProductVariant)
ORDER BY c.color_name;

SELECT DISTINCT 
    cs.category_size_id,
    cs.size_name
FROM dbo.Category_Size cs
WHERE cs.category_size_id IN (SELECT DISTINCT category_size_id FROM dbo.ProductVariant)
ORDER BY cs.size_name;
