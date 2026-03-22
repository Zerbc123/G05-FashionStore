# System Workflow

## 1. Overview
The Fashion Store system is an e-commerce platform built using the Spring Boot framework with an MVC architecture (Controller-Service-Repository). It utilizes a custom session-based authentication and authorization mechanism rather than Spring Security to handle access control across different user roles. The system manages product inventory, customer orders, staff accounts, and a real-time support chat system using WebSockets.

## 2. Actors
The system contains the following actors based on the `Role` entity and `RoleUtils` authorizations:
- **Admin**: Has full access to the system, including dashboard, orders, inventory, customers, staff management, and system reports.
- **Customer**: A registered client who can browse products, manage their profile, place orders, create support requests, and chat with support staff.
- **Guest**: An unauthenticated user who can browse products, view details, search for items, and access the login/registration pages.
- **Staff**: System employees, further categorized by specific roles:
  - **Quản lý cửa hàng (Manager)**: Has high-level access to dashboard, inventory, orders, and support.
  - **Nhân viên bán hàng (Sale)**: Has access to manage and view orders.
  - **Quản lý kho (Stock)**: Has access to view and manage inventory (products, variants, stock updates).
  - **Hỗ trợ khách hàng (Support)**: Handles customer support requests, updates statuses, and chats with customers.

## 3. Workflows

### 3.1 Manage Staff Accounts (Admin)
#### Main Flow
1. Admin logs into the system and navigates to the Staff Management Dashboard (`/admin/staff`).
2. System retrieves a paginated list of all active staff accounts and displays them.
3. Admin clicks "Add New Staff".
4. Admin fills in staff details (Username, Password, Email, Full Name, Phone) and selects a Role from the dropdown menu (Sale, Stock, Support, Manager).
5. Admin submits the form.
6. System validates the inputs (e.g., checks for duplicate phone/email).
7. System saves the new Account entity with the selected Role relationship.
8. System redirects Admin to the staff list with a success message.

#### Alternative Flow
- **Search Staff:** In Step 2, Admin enters a keyword to search. System retrieves filtered staff records.
- **Edit Staff:** Instead of adding, Admin clicks "Edit" on an existing staff member. Admin updates details or role and submits. The system updates the existing `Account` entity.
- **Soft Delete/Lock Staff:** Admin selects "Lock", "Leave", or "Delete" (move to trash). The system updates the account status without permanently deleting data.

#### Exception Flow
- **Validation Failure:** If Admin inputs invalid data (e.g., duplicated phone or empty name), the system returns to the form and displays validation errors.
- **Hard Delete Constraint Violation:** If Admin tries to permanently delete a staff member who is already assigned to active support requests, the system prevents deletion and shows a constraint error.

---

### 3.2 Manage Inventory (Admin / Stock Staff)
#### Main Flow
1. Actor (Admin or Stock Staff) logs in and navigates to the Inventory Management page (`/admin/inventory` or `/staff/inventory`).
2. System verifies if the session has `ROLE_ADMIN` or `ROLE_STOCK` permissions.
3. System retrieves all Product Variants with nested Product and Category entities using pagination.
4. System displays the inventory list, stock levels, and aggregated inventory stats (Total, In-Stock, Low-Stock, Out-of-Stock).

#### Alternative Flow
- **Filter Inventory:** Actor selects filters such as Category, Search text, or Stock Status (In-Stock, Low-Stock, Out-Stock). The System queries the database and dynamically displays the filtered list.

#### Exception Flow
- **Access Denied:** If an unauthorized actor (e.g., Sale Staff) attempts to access the page, the system throws an Access Denied message and redirects them.
- **Database Error:** If the system fails to fetch inventory products, an empty page is displayed alongside a general error message to prevent system crashes.

---

### 3.3 Handle Stock Operations (Admin / Stock Staff)
#### Main Flow
1. Actor (Admin or Stock Staff) is on the Inventory Management page.
2. Actor locates a specific Product Variant that requires a stock update.
3. Actor enters the new stock quantity and clicks the "Save/Update" button.
4. The request is sent to `/inventory/update-stock` with `variantId` and `newStock`.
5. System validates the actor's permissions (`canManageInventory`).
6. System finds the `ProductVariant` entity by ID, updates its `stock` value, and saves it to the database.
7. System redirects back to the inventory page with a success message.

#### Alternative Flow
- None (Straightforward data update).

#### Exception Flow
- **Variant Not Found:** If the provided Variant ID does not exist in the database, the system throws a RuntimeException and returns an error message to the UI.
- **Invalid Permission:** If the session expires or roles change, the system denies the action and redirects the user with an "Access Denied" flash message.

---

### 3.4 Manage Customer Support Requests
#### Main Flow (Customer Creating Request)
1. Customer logs in and navigates to the Support Page (`/customer/support`).
2. Customer fills in the Support Title and Description.
3. System extracts the Customer's name and email from their session.
4. System creates and saves a new `SupportRequest` with status `OPEN`.
5. Customer sees a success message and their request appears in "My Tickets".

#### Main Flow (Support Staff Handling Request)
1. Support Staff logs into the Staff Panel and accesses Support Tickets (`/staff/support`).
2. System retrieves tickets assigned to the Staff or OPEN tickets.
3. Staff views the ticket details and reads previous chat logs.
4. Staff can update the ticket status (e.g., `OPEN` to `IN_PROGRESS` or `RESOLVED`).
5. Staff can type a response in the chat interface.
6. The system saves the `SupportChat` message to the database and broadcasts the message via WebSocket to the Customer.
7. System marks previous messages sent by the Customer as "Read".

#### Alternative Flow
- **Admin Assignment:** Admin accesses `/admin/support` and manually assigns a specific `SupportRequest` to a specific Support Staff member (`staffId`).

#### Exception Flow
- **Ticket Access Violation (Customer):** If a Customer attempts to view a ticket ID belonging to a different customer, the system redirects them to "My Tickets" with an `access_denied` error.
- **Ticket Assign Violation (Staff):** If a Staff attempts to view a ticket assigned to a different staff member, the system returns a `not_assigned` error.

---

### 3.5 Authorize Request by Role
#### Main Flow
1. An incoming HTTP Request reaches a restricted Controller (e.g., `AdminController` or `StaffController`).
2. The Controller method invokes `RoleUtils` or internal role-check methods (e.g., `isStaff()`, `isAdmin()`).
3. The method retrieves `userRole` from the active `HttpSession`.
4. The method compares the retrieved role against the required constant (e.g., `RoleUtils.ROLE_ADMIN` or checks string contains for Staff variants).
5. If the condition is met, the system proceeds with executing the business logic.

#### Exception Flow
- **Unauthenticated / Role Missing:** If `userRole` is missing from the session, the system returns false for the authorization check and redirects the user to `/login`.
- **Role Mismatch:** If the user has a valid session but lacks the required role, the controller intercepts the action, prevents DB execution, and either displays a generic "Access Denied" page or redirects with an error flash attribute.
