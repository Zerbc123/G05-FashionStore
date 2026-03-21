-- Fix missing payment_method column in Orders table
-- This script adds the payment_method column that is defined in the Order entity

USE [fashion_shopping]
GO

-- Check if column exists before adding
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
               WHERE TABLE_NAME = 'Orders' 
               AND COLUMN_NAME = 'payment_method')
BEGIN
    ALTER TABLE [dbo].[Orders] 
    ADD [payment_method] NVARCHAR(50) NULL;
    
    PRINT 'Added payment_method column to Orders table';
END
ELSE
BEGIN
    PRINT 'payment_method column already exists';
END
GO

-- Update existing orders with default payment method
UPDATE [dbo].[Orders] 
SET [payment_method] = 'COD' 
WHERE [payment_method] IS NULL;

PRINT 'Updated existing orders with default payment method (COD)';
GO
