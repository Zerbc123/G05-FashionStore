-- Check ProductVariant table data
SELECT 
    pv.variant_id,
    pv.product_id,
    pv.color_id,
    c.color_name,
    pv.category_size_id,
    cs.size_name,
    pv.price,
    pv.stock,
    pv.image_url
FROM dbo.ProductVariant pv
LEFT JOIN dbo.Color c ON pv.color_id = c.color_id
LEFT JOIN dbo.Category_Size cs ON pv.category_size_id = cs.category_size_id
ORDER BY pv.product_id, pv.variant_id;

-- Check if there are any products
SELECT 
    p.product_id,
    p.product_name,
    p.category_id,
    cat.category_name
FROM dbo.Product p
LEFT JOIN dbo.Category cat ON p.category_id = cat.category_id
ORDER BY p.product_id;
