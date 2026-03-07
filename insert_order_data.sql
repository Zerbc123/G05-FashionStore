-- Insert sample data for Orders and OrderItem tables
-- Fashion Store Sample Orders Data

USE [fashion_shopping]
GO

-- Insert sample orders
SET IDENTITY_INSERT [dbo].[Orders] ON 
GO

INSERT [dbo].[Orders] ([order_id], [customer_id], [voucher_id], [account_id], [order_date], [status], [total_amount], [payment_status], [shipping_address]) 
VALUES (1, 1, NULL, 1, CAST(N'2026-01-15' AS Date), N'Completed', CAST(305000.00 AS Decimal(10, 2)), N'Paid', N'123 Nguyễn Huệ, Q.1, TP.HCM')
GO

INSERT [dbo].[Orders] ([order_id], [customer_id], [voucher_id], [account_id], [order_date], [status], [total_amount], [payment_status], [shipping_address]) 
VALUES (2, 2, NULL, 2, CAST(N'2026-01-18' AS Date), N'Completed', CAST(760000.00 AS Decimal(10, 2)), N'Paid', N'456 Lê Lợi, Q.3, TP.HCM')
GO

INSERT [dbo].[Orders] ([order_id], [customer_id], [voucher_id], [account_id], [order_date], [status], [total_amount], [payment_status], [shipping_address]) 
VALUES (3, 1, NULL, 1, CAST(N'2026-01-22' AS Date), N'Completed', CAST(570000.00 AS Decimal(10, 2)), N'Paid', N'789 Võ Văn Kiệt, Q.5, TP.HCM')
GO

INSERT [dbo].[Orders] ([order_id], [customer_id], [voucher_id], [account_id], [order_date], [status], [total_amount], [payment_status], [shipping_address]) 
VALUES (4, 2, NULL, 2, CAST(N'2026-01-25' AS Date), N'Completed', CAST(870000.00 AS Decimal(10, 2)), N'Paid', N'321 Trần Hưng Đạo, Q.1, TP.HCM')
GO

INSERT [dbo].[Orders] ([order_id], [customer_id], [voucher_id], [account_id], [order_date], [status], [total_amount], [payment_status], [shipping_address]) 
VALUES (5, 1, NULL, 1, CAST(N'2026-02-01' AS Date), N'Completed', CAST(440000.00 AS Decimal(10, 2)), N'Paid', N'654 Cách Mạng Tháng 8, Q.3, TP.HCM')
GO

INSERT [dbo].[Orders] ([order_id], [customer_id], [voucher_id], [account_id], [order_date], [status], [total_amount], [payment_status], [shipping_address]) 
VALUES (6, 2, NULL, 2, CAST(N'2026-02-05' AS Date), N'Completed', CAST(610000.00 AS Decimal(10, 2)), N'Paid', N'987 Nam Kỳ Khởi Nghĩa, Q.3, TP.HCM')
GO

INSERT [dbo].[Orders] ([order_id], [customer_id], [voucher_id], [account_id], [order_date], [status], [total_amount], [payment_status], [shipping_address]) 
VALUES (7, 1, NULL, 1, CAST(N'2026-02-10' AS Date), N'Completed', CAST(920000.00 AS Decimal(10, 2)), N'Paid', N'147 Đồng Khởi, Q.1, TP.HCM')
GO

INSERT [dbo].[Orders] ([order_id], [customer_id], [voucher_id], [account_id], [order_date], [status], [total_amount], [payment_status], [shipping_address]) 
VALUES (8, 2, NULL, 2, CAST(N'2026-02-15' AS Date), N'Completed', CAST(530000.00 AS Decimal(10, 2)), N'Paid', N'258 Nguyễn Trãi, Q.1, TP.HCM')
GO

INSERT [dbo].[Orders] ([order_id], [customer_id], [voucher_id], [account_id], [order_date], [status], [total_amount], [payment_status], [shipping_address]) 
VALUES (9, 1, NULL, 1, CAST(N'2026-02-20' AS Date), N'Completed', CAST(780000.00 AS Decimal(10, 2)), N'Paid', N'369 Hai Bà Trưng, Q.1, TP.HCM')
GO

INSERT [dbo].[Orders] ([order_id], [customer_id], [voucher_id], [account_id], [order_date], [status], [total_amount], [payment_status], [shipping_address]) 
VALUES (10, 2, NULL, 2, CAST(N'2026-02-25' AS Date), N'Completed', CAST(1130000.00 AS Decimal(10, 2)), N'Paid', N'741 Sư Vạn Hạnh, Q.10, TP.HCM')
GO

INSERT [dbo].[Orders] ([order_id], [customer_id], [voucher_id], [account_id], [order_date], [status], [total_amount], [payment_status], [shipping_address]) 
VALUES (11, 1, NULL, 1, CAST(N'2026-03-01' AS Date), N'Completed', CAST(670000.00 AS Decimal(10, 2)), N'Paid', N'852 Lý Thường Kiệt, Q.10, TP.HCM')
GO

INSERT [dbo].[Orders] ([order_id], [customer_id], [voucher_id], [account_id], [order_date], [status], [total_amount], [payment_status], [shipping_address]) 
VALUES (12, 2, NULL, 2, CAST(N'2026-03-05' AS Date), N'Completed', CAST(890000.00 AS Decimal(10, 2)), N'Paid', N'963 3 Tháng 2, Q.10, TP.HCM')
GO

SET IDENTITY_INSERT [dbo].[Orders] OFF 
GO

-- Insert sample order items
SET IDENTITY_INSERT [dbo].[OrderItem] ON 
GO

-- Order 1 items
INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (1, 1, 1, 2, CAST(150000.00 AS Decimal(10, 2)))
GO

INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (2, 1, 17, 1, CAST(180000.00 AS Decimal(10, 2)))
GO

-- Order 2 items
INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (3, 2, 3, 1, CAST(450000.00 AS Decimal(10, 2)))
GO

INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (4, 2, 26, 1, CAST(220000.00 AS Decimal(10, 2)))
GO

INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (5, 2, 30, 1, CAST(220000.00 AS Decimal(10, 2)))
GO

-- Order 3 items
INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (6, 3, 4, 1, CAST(380000.00 AS Decimal(10, 2)))
GO

INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (7, 3, 19, 1, CAST(120000.00 AS Decimal(10, 2)))
GO

INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (8, 3, 26, 1, CAST(220000.00 AS Decimal(10, 2)))
GO

-- Order 4 items
INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (9, 4, 7, 1, CAST(520000.00 AS Decimal(10, 2)))
GO

INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (10, 4, 9, 1, CAST(290000.00 AS Decimal(10, 2)))
GO

INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (11, 4, 26, 1, CAST(220000.00 AS Decimal(10, 2)))
GO

-- Order 5 items
INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (12, 5, 10, 1, CAST(290000.00 AS Decimal(10, 2)))
GO

INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (13, 5, 15, 1, CAST(220000.00 AS Decimal(10, 2)))
GO

-- Order 6 items
INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (14, 6, 12, 1, CAST(350000.00 AS Decimal(10, 2)))
GO

INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (15, 6, 26, 1, CAST(220000.00 AS Decimal(10, 2)))
GO

INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (16, 6, 30, 1, CAST(220000.00 AS Decimal(10, 2)))
GO

-- Order 7 items
INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (17, 7, 42, 1, CAST(850000.00 AS Decimal(10, 2)))
GO

INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (18, 7, 26, 1, CAST(220000.00 AS Decimal(10, 2)))
GO

-- Order 8 items
INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (19, 8, 36, 1, CAST(390000.00 AS Decimal(10, 2)))
GO

INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (20, 8, 26, 1, CAST(220000.00 AS Decimal(10, 2)))
GO

-- Order 9 items
INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (21, 9, 45, 1, CAST(420000.00 AS Decimal(10, 2)))
GO

INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (22, 9, 26, 1, CAST(220000.00 AS Decimal(10, 2)))
GO

INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (23, 9, 30, 1, CAST(220000.00 AS Decimal(10, 2)))
GO

-- Order 10 items
INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (24, 10, 42, 1, CAST(850000.00 AS Decimal(10, 2)))
GO

INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (25, 10, 26, 1, CAST(220000.00 AS Decimal(10, 2)))
GO

INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (26, 10, 30, 1, CAST(220000.00 AS Decimal(10, 2)))
GO

-- Order 11 items
INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (27, 11, 36, 1, CAST(390000.00 AS Decimal(10, 2)))
GO

INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (28, 11, 26, 1, CAST(220000.00 AS Decimal(10, 2)))
GO

INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (29, 11, 30, 1, CAST(220000.00 AS Decimal(10, 2)))
GO

-- Order 12 items
INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (30, 12, 7, 1, CAST(520000.00 AS Decimal(10, 2)))
GO

INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (31, 12, 26, 1, CAST(220000.00 AS Decimal(10, 2)))
GO

INSERT [dbo].[OrderItem] ([order_item_id], [order_id], [variant_id], [quantity], [price]) 
VALUES (32, 12, 30, 1, CAST(220000.00 AS Decimal(10, 2)))
GO

SET IDENTITY_INSERT [dbo].[OrderItem] OFF 
GO

-- Update product variant stock based on sales
UPDATE [dbo].[ProductVariant] SET stock = stock - 2 WHERE variant_id = 1
UPDATE [dbo].[ProductVariant] SET stock = stock - 1 WHERE variant_id = 17
UPDATE [dbo].[ProductVariant] SET stock = stock - 1 WHERE variant_id = 3
UPDATE [dbo].[ProductVariant] SET stock = stock - 1 WHERE variant_id = 26
UPDATE [dbo].[ProductVariant] SET stock = stock - 1 WHERE variant_id = 30
UPDATE [dbo].[ProductVariant] SET stock = stock - 1 WHERE variant_id = 4
UPDATE [dbo].[ProductVariant] SET stock = stock - 1 WHERE variant_id = 19
UPDATE [dbo].[ProductVariant] SET stock = stock - 1 WHERE variant_id = 7
UPDATE [dbo].[ProductVariant] SET stock = stock - 1 WHERE variant_id = 9
UPDATE [dbo].[ProductVariant] SET stock = stock - 1 WHERE variant_id = 10
UPDATE [dbo].[ProductVariant] SET stock = stock - 1 WHERE variant_id = 15
UPDATE [dbo].[ProductVariant] SET stock = stock - 1 WHERE variant_id = 12
UPDATE [dbo].[ProductVariant] SET stock = stock - 1 WHERE variant_id = 36
UPDATE [dbo].[ProductVariant] SET stock = stock - 1 WHERE variant_id = 42
UPDATE [dbo].[ProductVariant] SET stock = stock - 1 WHERE variant_id = 45
GO

PRINT 'Sample order data inserted successfully!'
GO
