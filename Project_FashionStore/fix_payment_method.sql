-- Fix missing payment_method column in orders table
-- Run this script in SQL Server Management Studio

USE [fashion_shopping]
GO

-- Check if column exists before adding
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'orders' 
               AND COLUMN_NAME = 'payment_method')
BEGIN
    ALTER TABLE [dbo].[orders] 
    ADD [payment_method] NVARCHAR(50) NULL;
    
    PRINT 'Added payment_method column to orders table';
END
ELSE
BEGIN
    PRINT 'payment_method column already exists';
END
GO

-- Update existing orders with default payment method
UPDATE [dbo].[orders] 
SET [payment_method] = 'COD' 
WHERE [payment_method] IS NULL;

PRINT 'Updated existing orders with default payment method (COD)';
GO
