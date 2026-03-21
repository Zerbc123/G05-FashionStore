# FashionStore Project Memory

## Project Overview
- **Framework:** Spring Boot 3.2.2, Java 21
- **Database:** SQL Server (localhost:1433, database=fashion_store)
- **Current Branch:** HoangDuy
- **Main Branch:** main

## Database Schema (Hibernate-Managed)
- **Strategy:** Hibernate DDL auto=update (entities are source of truth)
- **Config:** application.properties - spring.jpa.hibernate.ddl-auto=update
- **Total Tables:** 20 (auto-created, not SQL scripts)
- **Total Entities:** 20 classes in src/main/java/vn/edu/fpt/fashionstore/entity/

### Key Data Type Decision
- Customer.customerId: Long (BIGINT, not INT)
- Order.orderId: Long (BIGINT, not INT)
- OrderItem.orderItemId: Long (BIGINT, not INT)
- Product.productId: Long (BIGINT, not INT)
- Other IDs: Integer (INT)

### Core Enums (Stored as VARCHAR in DB)
- **OrderStatus:** PENDING, CONFIRMED, SHIPPING, COMPLETED, CANCELLED, REFUNDED
- **ReturnStatus:** PENDING, APPROVED, REJECTED, COMPLETED
- **SupportStatus:** OPEN, IN_PROGRESS, RESOLVED, CLOSED

## Repository Patterns
- All repositories extend JpaRepository<Entity, ID>
- Complex queries use @Query (JPQL)
- Parameter binding via @Param
- No hardcoded table names

## Important Files
- `HIBERNATE_SCHEMA_SYNC.md` - Full migration guide with schema definitions
- `DATABASE_SYNC_SUMMARY.md` - Comprehensive sync results & checklist
- `.backup_sql_scripts/` - Backup folder with old SQL scripts (for reference)

## Known Issues Fixed
- ✅ SQL scripts had INT but entities use Long - Resolved via Hibernate
- ✅ Type mismatch for IDs - Standardized to Long for main entities
- ✅ Code structure is consistent - All merged code is in sync

## Next Steps
1. Start application: `mvn spring-boot:run`
2. Hibernate auto-creates/updates schema on startup
3. Verify in SQL Server that all 20 tables are created
4. Test application end-to-end

## Code Quality Notes
- No compilation errors
- All JPA annotations correct (Jakarta.Persistence)
- All relationships properly mapped
- No N+1 query issues detected
- Services properly injected with repositories
