# System Use Case Descriptions

## UC-01: Manage Inventory

### Functional Description

| Field | Content |
|------|--------|
| UC ID and Name | UC-01: Manage Inventory |
| Created By | System |
| Date Created | 2026-03-23 |
| Primary Actor | Admin, Quản lý kho (Stock Staff) |
| Secondary Actors | Database |
| Description | Allows authorized staff to manage and monitor the overall product inventory, view key statistics, and search for specific product variants. |
| Trigger | Actor logs in and navigates to the Inventory Management page (`/admin/inventory` or `/staff/inventory`). |
| Preconditions | Actor must be authenticated and have `ROLE_ADMIN` or `Quản lý kho (Stock)` assigned to their session. |
| Postconditions | Actor views the updated list of inventory items and corresponding statistics. |

### Normal Flow
1. Actor clicks on the "Inventory" menu item.
2. System checks the role of the actor using `RoleUtils.canManageInventory(session)`.
3. System fetches aggregated inventory statistics (Total Products, In-Stock, Low-Stock, Out-of-Stock) via `InventoryService`.
4. System retrieves a paginated list of all `ProductVariant` records including their related `Product` and `Category`.
5. System displays the inventory management dashboard with statistics and a data table.

### Alternative Flows
- Case 1: Filter by Search, Category, or Status
  1. Actor enters a search keyword, selects a category, or chooses a stock status filter.
  2. Actor clicks the "Filter" button.
  3. System re-queries the database applying the submitted filters and displays the updated page.

### Exceptions
- Error case: Unauthorized Access
  1. Actor without permission (e.g., Sale) tries to access the direct URL.
  2. System detects unauthorized attempt and redirects to the Access Denied page or Dashboard.
- Error case: Data Fetch Error
  1. Database connection fails during query execution.
  2. System catches the exception, logs the error, and returns an empty table with an error flash message.

### Priority
High

### Business Rules
- BR1: Low-Stock is defined as having > 0 and <= 20 items.
- BR2: In-Stock is defined as having > 20 items.

### Other Information
- Uses JOIN FETCH to avoid lazy loading issues with Hibernate proxies.

---

## UC-02: View Inventory

### Functional Description

| Field | Content |
|------|--------|
| UC ID and Name | UC-02: View Inventory |
| Created By | System |
| Date Created | 2026-03-23 |
| Primary Actor | Staff (All Roles: Admin, Sale, Stock, Manager, Support) |
| Secondary Actors | Database |
| Description | Allows any staff member to view product availability and stock levels without necessarily having modification rights. |
| Trigger | Actor navigates to the Inventory view interface. |
| Preconditions | Actor must have an active staff session. |
| Postconditions | Actor successfully sees the current state of products and variants. |

### Normal Flow
1. Actor attempts to access the inventory page.
2. System invokes `RoleUtils.canViewInventory(session)` which verifies the actor is any type of staff.
3. System queries the `ProductVariantRepository` for paginated records.
4. System presents the inventory data table displaying Product Names, Categories, Prices, and Stock quantity.
5. The "Update Stock" button is visually hidden if the actor lacks `canManageInventory` permission.

### Alternative Flows
- Case 1: Navigation across pages
  1. Actor clicks on a pagination link (e.g., Page 2).
  2. System fetches the offset/limit rows using Spring Data Pageable and renders the next batch.

### Exceptions
- Error case: Session Timeout
  1. Actor's user session has expired.
  2. System intercepts the page request and redirects the user to `/login`.

### Priority
Medium

### Business Rules
- BR1: Read-only access should be granted to support and sales staff to assist customers accurately.
- BR2: Private product data should not be displayed to unauthenticated Guest users via this endpoint.

### Other Information
- Paginates defaults to 10 items per page.

---

## UC-03: Handle Stock Operations

### Functional Description

| Field | Content |
|------|--------|
| UC ID and Name | UC-03: Handle Stock Operations |
| Created By | System |
| Date Created | 2026-03-23 |
| Primary Actor | Admin, Quản lý kho (Stock Staff) |
| Secondary Actors | Database |
| Description | The process of directly modifying the available stock quantity for a specific product variant. |
| Trigger | Actor submits the stock update form for a specific variant row on the Inventory page. |
| Preconditions | Actor is on the Inventory Management page and has `canManageInventory` permission. |
| Postconditions | The targeted product variant has a new stock quantity, and the system reflects this globally. |

### Normal Flow
1. Actor locates the product variant they want to update on the list.
2. Actor enters a new integer value into the "Stock" input field.
3. Actor clicks "Update" or "Save".
4. System sends a POST request to `/inventory/update-stock` containing `variantId` and `newStock`.
5. System re-verifies the actor's permission through `RoleUtils`.
6. System retrieves the `ProductVariant` via `findById`.
7. System updates the `stock` attribute and executes `repository.save()`.
8. System redirects the actor to the inventory page with a success flash attribute.

### Alternative Flows
- None.

### Exceptions
- Error case: Invalid Variant ID
  1. Actor submits a Variant ID that does not map to any database record.
  2. `InventoryService` throws a `RuntimeException`.
  3. Controller catches the exception and redirects with an error flash message: "Lỗi khi cập nhật tồn kho...".

### Priority
High

### Business Rules
- BR1: Stock quantity cannot be negative (enforced by UI constraints or database schema validation).
- BR2: Only Admin or Stock Manager can update the quantities.

### Other Information
- Ensures atomicity by using repository-level save.

---

## UC-04: Assign Role to Account

### Functional Description

| Field | Content |
|------|--------|
| UC ID and Name | UC-04: Assign Role to Account |
| Created By | System |
| Date Created | 2026-03-23 |
| Primary Actor | Admin |
| Secondary Actors | Database |
| Description | Binding a specific role (Sale, Stock, Support, Manager) to a staff account during creation or modification. |
| Trigger | Admin submits an Add/Edit Staff form. |
| Preconditions | Admin is logged in. The `Role` dictionary implies valid IDs exist. |
| Postconditions | The target Staff `Account` entity holds a valid foreign key reference to a `Role`. |

### Normal Flow
1. Admin navigates to the Create Staff (`/admin/staff/create`) or Edit Staff (`/admin/staff/edit/{id}`) page.
2. Form requests the Admin to select a role from a populated dropdown.
3. Admin selects a role and submits the user form along with the `roleId`.
4. `StaffAccountController` receives the `Account` data and `roleId`.
5. System invokes `AccountService.createStaff(account, roleId)` or `updateStaffInfo(staff, roleId)`.
6. System fetches the exact `Role` entity using the `roleId`.
7. System associates the `Role` entity to the target `Account`.
8. System persists the `Account` object.

### Alternative Flows
- Case 1: Edit Account without Changing Role
  1. Admin opens the Edit Staff form but leaves the existing role selected.
  2. System processes the submission, re-assigning the same role without any logical disruption.

### Exceptions
- Error case: Invalid Role ID
  1. A malformed `roleId` is received during POST.
  2. Service fails to resolve the `Role` entity.
  3. System rejects creation/update and throws an error informing the Admin.

### Priority
High

### Business Rules
- BR1: Every Staff Account must have exactly one associated active Role.
- BR2: Customers do not belong in the Staff Account list; they obtain the "Customer" role implicitly via standard registration.

### Other Information
- Managed through Hibernate `@ManyToOne` mapping on `Account.role`.

---

## UC-05: Manage Staff Accounts

### Functional Description

| Field | Content |
|------|--------|
| UC ID and Name | UC-05: Manage Staff Accounts |
| Created By | System |
| Date Created | 2026-03-23 |
| Primary Actor | Admin |
| Secondary Actors | System Notifications (Optional), Database |
| Description | Entire lifecycle administration of staff: Listing, Searching, Creating, Soft Deleting, Hard Deleting, and Restoring. |
| Trigger | Admin accesses the Staff Management Dashboard. |
| Preconditions | Admin is authenticated and has the `ROLE_ADMIN` entitlement. |
| Postconditions | The staff registry is successfully manipulated or queried by the Admin. |

### Normal Flow
1. Admin opens `/admin/staff`.
2. System loads users dynamically in a paginated list using `AccountService.getAllStaff`.
3. Admin interacts with a row to perform an action (Lock, Unlock, Leave, Disable).
4. For locking: Admin clicks "Lock".
5. System endpoints catch the request (e.g., `/admin/staff/lock/{id}`).
6. System changes the staff `status` flag.
7. System reloads the view.

### Alternative Flows
- Case 1: Hard Deletion of Staff
  1. Admin goes to Trash bin view (`/admin/staff/trash`).
  2. Admin clicks "Hard Delete" on a Staff member.
  3. System checks if foreign keys (e.g., Support Requests assigned to this staff) exist using `isStaffAssigned(id)`.
  4. If false, the system executes a permanent SQL delete against the staff record.

### Exceptions
- Error case: Foreign Key Constraint Violation on Deletion
  1. Admin attempts to Hard Delete a Staff member dynamically assigned to active Support Requests.
  2. Database rejects the transaction due to referencing tuples.
  3. Controller catches the foreign key constraint exception.
  4. System prevents deletion and warns the Admin that the account is currently tied to customer requests.
- Error case: Duplicate Phone or Email
  1. Admin tries to create a staff member with a phone number that already belongs to another account.
  2. System returns an error: "Số điện thoại đã tồn tại".

### Priority
High

### Business Rules
- BR1: Phone numbers and Emails must be distinct across the entire Account table.
- BR2: Staff actively handling tickets cannot be hard deleted; they can only be deactivated (Locked).

### Other Information
- Features real-time checking for duplicate phone numbers via an AJAX endpoint `/check-phone-duplicate`.

---

## UC-06: Manage Customer Support Requests

### Functional Description

| Field | Content |
|------|--------|
| UC ID and Name | UC-06: Manage Customer Support Requests |
| Created By | System |
| Date Created | 2026-03-23 |
| Primary Actor | Customer, Hỗ trợ khách hàng (Support Staff), Admin |
| Secondary Actors | WebSocket Manager, Database |
| Description | Complete workflow from a Customer opening a support ticket to the Support Staff claiming and interacting with the Customer via instant messaging, including status lifecycle management. |
| Trigger | Customer submits a new request, or Staff logs into the support dashboard. |
| Preconditions | Valid session matching the appropriate role (Customer, Support, or Admin). |
| Postconditions | A ticket is successfully pushed through the pipeline (`OPEN` -> `IN_PROGRESS` -> `RESOLVED`) and chat history is preserved. |

### Normal Flow
1. Customer visits `/customer/support` and creates a request linking their email and full name.
2. System initializes a `SupportRequest` record with status `OPEN`.
3. Support Staff visits `/staff/support` and opens the ticket details `/view/{id}`.
4. If unassigned, Staff can update the status to `IN_PROGRESS`.
5. Staff enters text in the chat box and submits.
6. System saves the `SupportChat` record associating `SenderType.STAFF`.
7. System broadcasts the message payload natively utilizing a WebSocket endpoint `chatWebSocketController` to instantly notify the listening Customer client.
8. Customer receives the message on their UI, replies, and logs a `SenderType.CUSTOMER` message.
9. System marks messages as read (`markStaffMessagesAsRead` or `markCustomerMessagesAsRead`).

### Alternative Flows
- Case 1: Admin Routing
  1. An Admin logs in, looks at `/admin/support/view/{id}`.
  2. Admin uses the Assignment drop-down to manually tie a `staffId` to the `SupportRequest`.
  3. The chosen Support Staff is instantly designated via `supportRequestService.assignStaff`.

### Exceptions
- Error case: Access Violation (Customer Cross-talk)
  1. A Customer attempts to open `/view/{id}` for a Ticket ID belonging to another user.
  2. System checks `supportRequest.getCustomerEmail().equals(customer.getEmail())`.
  3. Operation is denied and Customer is redirected to "My Tickets".
- Error case: Staff Modifying Unassigned Ticket
  1. Staff views a ticket forcibly linked to someone else by Admin.
  2. Upon submitting a status change or chat, backend logic catches the `assignedStaffId` disparity.
  3. Request drops and redirects with "Bạn không có quyền cập nhật yêu cầu này".

### Priority
High

### Business Rules
- BR1: Support interactions must be securely siloed (Customers can only view their own tickets; Staff can only alter tickets assigned to them).
- BR2: Statuses are constrained to the bounds of the `SupportStatus` enumeration.

### Other Information
- WebSocket pushes notifications in real-time, eliminating manual page-refresh looping.

---

## UC-07: Authorize Request by Role

### Functional Description

| Field | Content |
|------|--------|
| UC ID and Name | UC-07: Authorize Request by Role |
| Created By | System |
| Date Created | 2026-03-23 |
| Primary Actor | System (RoleUtils / Controller Gates) |
| Secondary Actors | HttpSession |
| Description | A generic, custom session-oriented security filter process occurring before executing business logic, verifying the inbound actor maintains the necessary role. |
| Trigger | System receives an HTTP request routed to an internal Controller (`/admin/**`, `/staff/**`, `/customer/**`). |
| Preconditions | The Spring Interceptor/Security Configuration allows the endpoint to process freely (since standard FormLogin is disabled). |
| Postconditions | The controller either gracefully executes or preemptively terminates the business processing cycle. |

### Normal Flow
1. An Actor initiates an endpoint (e.g., `GET /staff/dashboard`).
2. The Controller method triggers local authorization helper `isStaff(HttpSession session)` or identical functions.
3. The method extracts the internal session property: `session.getAttribute("userRole")`.
4. System leverages the `RoleUtils` to validate the retrieved string against hard-coded literals (e.g., "Quản lý cửa hàng (Manager)").
5. The string verification succeeds (returns `true`).
6. Core business methods are evaluated.

### Alternative Flows
- Case 1: Broad Role Capabilities Check
  1. A feature requires multiple roles (e.g. `RoleUtils.canViewOrders`).
  2. The function yields `true` evaluating multiple OR constraints (`isAdmin || isSale || isManager`).

### Exceptions
- Error case: Unauthorized Activity
  1. The user string lacks adequate permission (e.g., a "Customer" hits a "Staff" layout).
  2. The boolean check returns `false`.
  3. Action drops instantly, rendering an HTTP redirect to `/login` or displaying a localized "Access Denied" page depending on the UX strategy.

### Priority
Critical

### Business Rules
- BR1: Role evaluation is primarily processed as hard-coded string comparisons corresponding to the actual strings assigned in the database (`Role` records mapping).
- BR2: Unauthenticated properties return `null` and therefore automatically fail checks.

### Other Information
- Replaces standard Spring Security Authentication context checks. Heavily reliant on stable `HttpSession` preservation.
