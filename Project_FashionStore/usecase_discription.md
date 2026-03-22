# System Use Case Descriptions (Fashion Store E-commerce)

Dưới đây là danh sách phân tích các Use Case (Luồng sử dụng) chi tiết của hệ thống theo đúng định dạng bảng yêu cầu, dựa vào tập tài liệu `System_Workflow.md` đã phân tích trước đó.

---

## 1. Authentication & Authorization Flow

### UC-01.1 Register New Account
| Functional Description | |
| --- | --- |
| **ID and Name:** | UC-01.1 Register New Account |
| **Created By:** | System Analyst |
| **Date Created:** | 23/03/2026 |
| **Primary Actor:** | Guest |
| **Secondary Actors:** | System (Email Service) |
| **Description:** | This use case allows an unregistered user to create a new Customer account using their email and password. |
| **Trigger:** | The Guest clicks the "Register" button on the Login/Register UI. |
| **Preconditions:** | The Guest is not successfully logged into the system.<br>The system database is available. |
| **Postconditions:** | A new Customer account is created in the database.<br>An onboarding confirmation email is sent to the customer. |
| **Normal Flow:** | 1. Guest clicks the "Register" menu item.<br>2. System displays the registration form.<br>3. Guest enters email, password, and personal information (Name, Phone).<br>4. Guest submits the form.<br>5. System validates the input data for correctness and uniqueness.<br>6. System hashes the password and creates a new Account record.<br>7. System triggers the Email Service to send a confirmation email.<br>8. System redirects the user to the Login screen with a success message. |
| **Alternative Flows:** | **Email already exists:**<br>1. System detects the email is already registered.<br>2. System displays an error message: "Email already in use".<br>3. System prompts Guest to login or use a different email. |
| **Exceptions:** | **Database/Connection Failure:**<br>1. System fails to save the account due to an internal error.<br>2. System logs the error for technical investigation.<br>3. System displays: "Registration failed. Please try again later." |
| **Priority:** | High |
| **Business Rules:** | BR1, BR2 |
| **Other Information:** | Passwords must be encrypted before database insertion. Customer accounts are assigned the role `CUSTOMER` by default. |

**Business Rules**
| Rule ID | Rule Name | Description |
| --- | --- | --- |
| BR1 | Unique Email | Each account must map to a unique email address. |
| BR2 | Strong Password | Passwords must satisfy security complexity requirements. |

<br>

### UC-01.2 Login to System
| Functional Description | |
| --- | --- |
| **ID and Name:** | UC-01.2 Login to System |
| **Created By:** | System Analyst |
| **Date Created:** | 23/03/2026 |
| **Primary Actor:** | Customer, Admin, Staff |
| **Secondary Actors:** | OAuth2 Provider (Google/Facebook) |
| **Description:** | This use case allows registered users to authenticate and access their role-specific systems. |
| **Trigger:** | The user clicks "Login" and submits their credentials. |
| **Preconditions:** | The user has an existing account in the system.<br>The system database is available. |
| **Postconditions:** | A session is established with the correct authorization roles. |
| **Normal Flow:** | 1. User navigates to the Login page.<br>2. User enters email and password and submits details.<br>3. System verifies credentials against the database.<br>4. System establishes an authenticated session.<br>5. System redirects user to the Dashboard/Homepage based on their Role. |
| **Alternative Flows:** | **Login via Single Sign-on (OAuth2):**<br>1. User clicks "Login with Google".<br>2. System delegates authentication to OAuth2 Provider.<br>3. Provider returns user authentication payload.<br>4. System links payload to existing account or creates a new one.<br>5. System establishes session and redirects. |
| **Exceptions:** | **Invalid Credentials:**<br>1. System determines credentials do not match any active record.<br>2. System displays "Invalid username or password".<br>**Account Suspended:**<br>1. System detects account is inactive/disabled.<br>2. System displays "Your account is locked." |
| **Priority:** | High |
| **Business Rules:** | BR3 |
| **Other Information:** | The system leverages Spring Security for Authentication management. |

**Business Rules**
| Rule ID | Rule Name | Description |
| --- | --- | --- |
| BR3 | Role-Based Access | Users are only authorized to access pages associated with their Roles (Customer, Admin, Staff). |

---

## 2. Shopping & Checkout Flow

### UC-02.1 Add Product to Cart
| Functional Description | |
| --- | --- |
| **ID and Name:** | UC-02.1 Add Product to Cart |
| **Created By:** | System Analyst |
| **Date Created:** | 23/03/2026 |
| **Primary Actor:** | Customer |
| **Secondary Actors:** | None |
| **Description:** | This use case describes how a user selects a product variant (size, color) and adds it to their shopping cart. |
| **Trigger:** | User clicks the "Add to Cart" button on the Product Details page. |
| **Preconditions:** | The selected product is active and in stock. |
| **Postconditions:** | The Cart is updated with the selected product variant and quantity. |
| **Normal Flow:** | 1. User views a product detail page.<br>2. User selects desired Color and Size (affecting ProductVariant).<br>3. User specifies the Quantity numerically.<br>4. User clicks "Add to Cart".<br>5. System verifies the required stock is available in inventory.<br>6. System creates a CartItem linking the Customer and ProductVariant.<br>7. System updates the Cart icon counter and displays a success notification. |
| **Alternative Flows:** | **Product variant already in Cart:**<br>1. System detects the exact variant is already inside the cart.<br>2. System increases the quantity of the existing CartItem instead of creating a new row.<br>3. System notifies user of the updated quantity. |
| **Exceptions:** | **Insufficient Stock:**<br>1. System detects the requested quantity exceeds available stock.<br>2. System displays an error: "Only X items left in stock". |
| **Priority:** | High |
| **Business Rules:** | BR4, BR5 |
| **Other Information:** | Cart items for guests (if allowed) are stored in session before login. |

**Business Rules**
| Rule ID | Rule Name | Description |
| --- | --- | --- |
| BR4 | Stock Validation | A product cannot be added if requested quantity > current physical stock. |
| BR5 | Distinct Variant Separation | Different configurations (e.g. Red-S vs Blue-M) of the same Product ID count as distinct Cart Items. |

<br>

### UC-02.2 Checkout Order
| Functional Description | |
| --- | --- |
| **ID and Name:** | UC-02.2 Checkout Order |
| **Created By:** | System Analyst |
| **Date Created:** | 23/03/2026 |
| **Primary Actor:** | Customer |
| **Secondary Actors:** | Payment Gateway (Momo) |
| **Description:** | Customers review their cart, select shipping/payment methods, apply vouchers, and place an order. |
| **Trigger:** | Customer clicks "Proceed to Checkout" from the Cart View. |
| **Preconditions:** | The Customer is authenticated and has at least one valid item in their Cart. |
| **Postconditions:** | An Order record is created. Cart is emptied. Product Inventory is reduced. |
| **Normal Flow:** | 1. Customer initiates checkout.<br>2. System retrieves CartItems and prompts for Shipping Address.<br>3. Customer selects/enters Address and chooses Payment Method (COD or Momo).<br>4. (Optional) Customer inputs and applies a Voucher code.<br>5. System calculates final Subtotal, Discount, Shipping, and Total Price.<br>6. Customer clicks "Place Order".<br>7. System verifies stock, creates Order and OrderItems records.<br>8. System clears the Cart.<br>9. System redirects to Success page (if COD) or Payment Gateway (if Momo). |
| **Alternative Flows:** | **Failed Payment Gateway Initiation:**<br>1. Customer chooses to pay with Momo.<br>2. System fails to generate MomoRequestDTO due to timeout.<br>3. System aborts Order creation and displays: "Payment Gateway unavailable. Try again later." |
| **Exceptions:** | **Price/Stock changed during checkout:**<br>1. System detects stock is no longer available during final submit.<br>2. System halts order creation and prompts: "Some items in your cart are out of stock."<br>3. System removes unavailable items and returns Customer to Cart. |
| **Priority:** | Critical |
| **Business Rules:** | BR6, BR7 |
| **Other Information:** | The system executes this as an atomic transaction to ensure no stranded orders are created if Cart clearing fails. |

**Business Rules**
| Rule ID | Rule Name | Description |
| --- | --- | --- |
| BR6 | Empty Cart Validation | Checkout cannot be initiated with an empty cart. |
| BR7 | Voucher Usage Criteria | A voucher can only be applied and calculated if order subtotal satisfies voucher minimum criteria. |

---

## 3. Order Management Flow

### UC-03.1 View Order History
| Functional Description | |
| --- | --- |
| **ID and Name:** | UC-03.1 View Order History |
| **Created By:** | System Analyst |
| **Date Created:** | 23/03/2026 |
| **Primary Actor:** | Customer |
| **Secondary Actors:** | None |
| **Description:** | This use case allows a Customer to view the list of orders that they have previously placed in the system. The orders displayed include those that have been successfully created and assigned an Order ID but are pending confirmation by Admin. |
| **Trigger:** | The Customer clicks the "Order History" option from the user menu on the dashboard. |
| **Preconditions:** | The Customer has successfully logged into the system.<br>The Customer account is active.<br>The system database is available. |
| **Postconditions:** | The system displays a list of orders belonging to the Customer.<br>Each order shows basic information (Order ID, date, total amount, status).<br>No system data is modified. |
| **Normal Flow:** | 1. Customer clicks the "Order History" menu item.<br>2. System receives the request and verifies the Customer's authentication session.<br>3. System retrieves all orders associated with the Customer's account from the database.<br>4. System filters orders to include those created by the Customer, including orders pending confirmation.<br>5. System sorts the order list by creation date in descending order.<br>6. System displays the history list on the Order History screen.<br>7. Customer views the displayed history. |
| **Alternative Flows:** | **Customer has no order history:**<br>1. System determines that no orders exist for the account.<br>2. System displays informational message: "You have not placed any orders yet."<br>3. System displays an empty Order History screen. |
| **Exceptions:** | **User session expired:**<br>1. System detects invalid/expired session.<br>2. System redirects Customer to the Login screen.<br>3. System displays an error message: "Your session has expired. Please log in again."<br>**Database retrieval failure:**<br>1. System fails to retrieve data due to database error.<br>2. System logs error.<br>3. System displays: "Unable to load order history at this time." |
| **Priority:** | Medium |
| **Business Rules:** | BR8, BR9 |
| **Other Information:** | Ensure data confidentiality to prevent unauthorized access to other customers' data. |

**Business Rules**
| Rule ID | Rule Name | Description |
| --- | --- | --- |
| BR8 | Order Ownership | A Customer can only view orders created by their own authenticated account. |
| BR9 | Data Integrity | Order data displayed must reflect the latest status stored in the system database. |

<br>

### UC-03.2 Update Order Status (Admin/Staff)
| Functional Description | |
| --- | --- |
| **ID and Name:** | UC-03.2 Update Order Status |
| **Created By:** | System Analyst |
| **Date Created:** | 23/03/2026 |
| **Primary Actor:** | Staff, Admin |
| **Secondary Actors:** | System (Email Service) |
| **Description:** | Allows assigned Staff or Admins to update the state of customer orders as they are processed, shipped, and delivered. |
| **Trigger:** | Staff selects a new status and clicks "Update" on an Order Details administrative page. |
| **Preconditions:** | Staff/Admin is authenticated with correct Role.<br>The selected Order exists. |
| **Postconditions:** | Order status is successfully updated in the database.<br>Customer is notified via Email. |
| **Normal Flow:** | 1. Staff navigates to the Order Management view.<br>2. Staff views specific Order details.<br>3. Staff selects a new valid OrderStatus (e.g., Pending to Shipped).<br>4. Staff clicks "Submit/Update".<br>5. System validates the transition.<br>6. System updates Order status.<br>7. System triggers Email Service to dispatch a status update email to Customer.<br>8. System displays Success message to staff. |
| **Alternative Flows:** | **Admin Cancels Order:**<br>1. Staff selects "Canceled" status.<br>2. System requires a cancellation rationale.<br>3. Staff inputs reason and confirms.<br>4. System updates order status to Canceled.<br>5. System automatically restores the deducted product inventory.<br>6. Email notification is dispatched. |
| **Exceptions:** | **Invalid Status Transition:**<br>1. System detects staff trying to move an unchangeable state (e.g., "Delivered" back to "Pending").<br>2. System halts operation and displays error: "Invalid status transition. Action denied." |
| **Priority:** | High |
| **Business Rules:** | BR10, BR11 |
| **Other Information:** | Canceling an order paid via Momo may require manual refund tracking. |

**Business Rules**
| Rule ID | Rule Name | Description |
| --- | --- | --- |
| BR10 | Valid Status Transitions | Orders cannot skip logical states randomly (e.g., from Pending directly to Delivered). |
| BR11 | Inventory Restoration | Canceling a confirmed order must restore stock quantities representing the OrderItems. |

---

## 4. Product Catalog Management Flow

### UC-04.1 Manage Products (CRUD)
| Functional Description | |
| --- | --- |
| **ID and Name:** | UC-04.1 Manage Products |
| **Created By:** | System Analyst |
| **Date Created:** | 23/03/2026 |
| **Primary Actor:** | Admin |
| **Secondary Actors:** | None |
| **Description:** | Admin can Add, Edit, or Delete (Deactivate) products and their nested variants (Size, Color) to keep the store catalog up to date. |
| **Trigger:** | Admin clicks "Products" tab in the Admin Dashboard. |
| **Preconditions:** | Admin is authenticated and authorized with `ADMIN` role. |
| **Postconditions:** | The catalog is updated and changes are reflected immediately on the public storefront. |
| **Normal Flow:** | 1. Admin navigates to the Product List.<br>2. Admin clicks "Create New Product".<br>3. System displays product entry form.<br>4. Admin inputs metadata (Name, Description), uploads images, limits Categories, and binds Variants (Sizes, Colors, Stock count).<br>5. Admin clicks "Save".<br>6. System validates the input data.<br>7. System inserts new Product and associated ProductVariants into the database.<br>8. System routes Admin back to list with success toast notification. |
| **Alternative Flows:** | **Update Existing Product Details:**<br>1. Admin clicks "Edit" on an active product.<br>2. Admin alters pricing or current stock.<br>3. Admin saves changes.<br>4. System updates specific records in database seamlessly without breaking related historical Orders. |
| **Exceptions:** | **Duplicate SKU / Blank Mandatory Field:**<br>1. System detects missing vital data or collision with an existing SKU/Variant Name.<br>2. System prevents saving and visually highlights problematic form fields. |
| **Priority:** | High |
| **Business Rules:** | BR12 |
| **Other Information:** | Image uploads must be sanitized before persisting to the filesystem/storage. |

**Business Rules**
| Rule ID | Rule Name | Description |
| --- | --- | --- |
| BR12 | Soft Deletion Only | A product/variant cannot be permanently deleted via SQL DELETE if it is tied to an existing OrderItem; it must be marked as `inactive` instead. |
