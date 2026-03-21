-- Add Foreign Key Constraint for support_request.assigned_staff_id -> Account.account_id
-- This will enforce referential integrity when deleting accounts

-- First, check if constraint already exists and drop it if needed
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID('support_request') AND name = 'FK_support_request_Account')
BEGIN
    ALTER TABLE support_request DROP CONSTRAINT FK_support_request_Account;
    PRINT 'Dropped existing FK constraint: FK_support_request_Account';
END

-- Add foreign key constraint with NO ACTION (application will handle validation)
ALTER TABLE support_request 
ADD CONSTRAINT FK_support_request_Account 
FOREIGN KEY (assigned_staff_id) 
REFERENCES Account(account_id) 
ON DELETE NO ACTION -- Prevent deletion if staff is assigned to support requests
ON UPDATE CASCADE; -- Update assigned_staff_id when account_id changes

PRINT 'Added FK constraint: FK_support_request_Account';
PRINT 'Note: Application will handle validation before deletion';

-- Verify: Check for staff accounts that have assigned support requests
SELECT 
    a.account_id,
    a.username,
    a.email,
    COUNT(sr.id) as assigned_support_requests
FROM Account a
LEFT JOIN support_request sr ON a.account_id = sr.assigned_staff_id
WHERE a.account_id IN (
    SELECT DISTINCT assigned_staff_id 
    FROM support_request 
    WHERE assigned_staff_id IS NOT NULL
)
GROUP BY a.account_id, a.username, a.email
HAVING COUNT(sr.id) > 0
ORDER BY COUNT(sr.id) DESC;

-- Verify: Get support requests assigned to staff
SELECT 
    sr.id,
    sr.title,
    sr.customer_name,
    sr.customer_email,
    sr.assigned_staff_id,
    sr.assigned_staff_name,
    sr.status
FROM support_request sr
WHERE sr.assigned_staff_id IS NOT NULL
ORDER BY sr.assigned_staff_id, sr.created_at DESC;

-- Verify the constraint was added
SELECT 
    fk.name AS ForeignKeyName,
    tp.name AS ParentTable,
    cp.name AS ParentColumn,
    tr.name AS ReferencedTable,
    cr.name AS ReferencedColumn,
    fk.delete_referential_action_desc AS DeleteAction,
    fk.update_referential_action_desc AS UpdateAction
FROM sys.foreign_keys fk
INNER JOIN sys.tables tp ON fk.parent_object_id = tp.object_id
INNER JOIN sys.tables tr ON fk.referenced_object_id = tr.object_id
INNER JOIN sys.foreign_key_columns fkc ON fk.object_id = fkc.constraint_object_id
INNER JOIN sys.columns cp ON fkc.parent_object_id = cp.object_id AND fkc.parent_column_id = cp.column_id
INNER JOIN sys.columns cr ON fkc.referenced_object_id = cr.object_id AND fkc.referenced_column_id = cr.column_id
WHERE tp.name = 'support_request' AND tr.name = 'Account';
