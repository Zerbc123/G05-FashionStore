# Project Use Case Specification

## UC-01: Login

**Actors:** Customer, Staff, Admin, Google Service  
**Description:** This use case describes how an authorized user accesses the system by entering valid credentials or authenticating via Google. The system establishes a session and redirects the user to their role-appropriate home page.  

**Preconditions:**
- The user is not currently authenticated.
- The system and Authentication Service are operational.

**Main Flow:**
1. User navigates to the Login page and enters username/email and password.
2. User clicks the "Login" button.
3. System receives the request and verifies credentials.
4. System checks if the account status is "active".
5. System creates an authentication session and identifies the user role.
6. System redirects the user to the corresponding home page (Admin dashboard, Staff dashboard, or Customer home).

**Alternative Flows:**
- **Login via Google:**
  - User clicks "Login with Google".
  - System redirects user to Google for authentication.
  - Upon success, System retrieves user identity.
  - If it's a new user, System automatically registers them as a Customer.
  - If the user is missing phone/address details, System redirects them to the Edit Profile page.

**Postconditions:**
- The user is successfully authenticated.
- A valid session is created.
- User is redirected based on role.

**Business Rules:**
- **BR1:** Username and password must be matched with active account records.
- **BR2:** Users are granted access based on assigned roles (Customer, Staff, Admin).
- **BR3:** Only valid and active accounts can be authenticated.

---

## UC-02: Change Password

**Actors:** Customer  
**Description:** Allows an authenticated Customer to secure their account by changing their password.

**Preconditions:**
- The Customer is authenticated and has an active session.
- The system database is operational.

**Main Flow:**
1. Customer accesses the Change Password page.
2. Customer enters their current password, new password, and confirms the new password.
3. Customer clicks "Save Changes".
4. System verifies the current password matches the stored password.
5. System verifies the new password meets policy requirements.
6. System verifies the new password matches the confirmation password.
7. System updates and securely hashes the new password in the database.
8. System displays a success message.

**Alternative Flows:**
- None.

**Postconditions:**
- The Customer's password is successfully updated in the database.

**Business Rules:**
- **BR1:** Current password must match the hashed password in the database.
- **BR2:** New password must be 8-12 characters long, including at least 1 uppercase letter, 1 lowercase letter, 1 number, and may contain special characters.
- **BR3:** New password and confirm password must match exactly.

---

## UC-03: Logout

**Actors:** Customer, Staff, Admin  
**Description:** Describes how an authenticated user terminates their active session securely.

**Preconditions:**
- The user is authenticated and has an active session.

**Main Flow:**
1. User clicks the "Logout" option on the system interface.
2. System receives the request and invalidates the user's HTTP session.
3. System clears authentication-related data via the Security Context.
4. System redirects the user to the Login page with a logout parameter.

**Alternative Flows:**
- None.

**Postconditions:**
- The user session is invalidated.
- User is redirected to the Login page.

**Business Rules:**
- **BR1:** Logout must immediately invalidate the session and clear all authentication tokens.
- **BR2:** Logout behavior must be consistent across all roles (Customer, Staff, Admin).

---

## UC-04: Payment

**Actors:** Customer, MoMo Payment Gateway  
**Description:** Describes how a Customer completes an order payment using the MoMo payment gateway.

**Preconditions:**
- Customer is authenticated.
- Order exists with calculated total amount.

**Main Flow:**
1. Customer initiates payment for an order.
2. System creates a payment request with a unique request ID and order information.
3. System sends the request to the MoMo Payment Gateway.
4. System redirects Customer to the MoMo payment URL.
5. Customer completes the transaction on the MoMo platform.
6. MoMo redirects the Customer back via the Return URL and sends an IPN (Instant Payment Notification) via webhook.
7. System verifies the MoMo signature to ensure data integrity.
8. System updates the order payment status upon successful verification.
9. System redirects the Customer to the success page.

**Alternative Flows:**
- **Payment Failed / Cancelled:**
  - If MoMo returns a result code other than "0", the system marks the payment as failed and redirects the Customer to the checkout error page.

**Postconditions:**
- Payment transaction result is securely verified.
- Order status is updated appropriately.

**Business Rules:**
- **BR1:** All payment responses from MoMo must have their signatures strictly verified.
- **BR2:** A return code of "0" indicates a successful payment.

---

## UC-05: View Order History

**Actors:** Customer  
**Description:** Allows an authenticated Customer to view the list of their previously placed orders.

**Preconditions:**
- Customer is authenticated and has an active session.

**Main Flow:**
1. Customer navigates to the Order History section.
2. System verifies the Customer's session.
3. System retrieves all orders belonging to the Customer from the database.
4. System sorts the orders by creation date in descending order (newest first).
5. System displays the list of orders with their IDs, dates, statuses, and total amounts.

**Alternative Flows:**
- **No Orders Found:**
  - If the Customer has no orders, the system displays an empty list.

**Postconditions:**
- System displays the correct list of orders belonging solely to the authenticated Customer.

**Business Rules:**
- **BR1:** A Customer must only be allowed to view orders associated with their own account.
- **BR2:** Orders must be displayed in descending order by date.

---

## UC-06: View Order Detail

**Actors:** Customer  
**Description:** Allows a Customer to view detailed information regarding a specific order.

**Preconditions:**
- Customer is authenticated.
- The Order ID exists and belongs to the authenticated Customer.

**Main Flow:**
1. Customer selects a specific order to view details.
2. System receives the request and verifies the Order ID.
3. System strictly verifies that the selected Order belongs to the querying Customer.
4. System retrieves the order details and the list of order items (products, variants, quantities, prices).
5. System retrieves the delivery address used for the order.
6. System formats and displays the order detail screen.

**Alternative Flows:**
- **Admin/Staff Viewing:**
  - If an Admin or Staff accesses the order details, the system bypasses the customer-ownership check and displays management action buttons.

**Postconditions:**
- Detailed order information is displayed without modification.

**Business Rules:**
- **BR1:** A Customer can only view details of orders created by their own account.
- **BR2:** The displayed data must accurately reflect the stored order status, order items, and delivery address.

---

## UC-07: Track Order Status

**Actors:** Customer  
**Description:** Allows a Customer to track the real-time status of their order.

**Preconditions:**
- Customer is authenticated.
- Order exists and belongs to the Customer.

**Main Flow:**
1. Customer accesses the Order History or Order Detail page.
2. System retrieves the latest order status (e.g., PENDING, CONFIRMED, CANCELLED) from the database.
3. System displays the corresponding status badge and message on the user interface.

**Alternative Flows:**
- None.

**Postconditions:**
- The correct order status is visible to the Customer.

**Business Rules:**
- **BR1:** The displayed status must exactly match the `status` field defined in the Order entity.

---

## UC-08: Cancel Order Request

**Actors:** Customer  
**Description:** Allows a Customer to cancel an order, provided it is still pending confirmation.

**Preconditions:**
- Customer is authenticated.
- The order belongs to the Customer.
- The order status is `PENDING`.

**Main Flow:**
1. Customer clicks the "Cancel" button for a specific order.
2. System verifies that the order belongs to the Customer.
3. System verifies that the order's current status allows cancellation (must be `PENDING`).
4. System updates the order status to `CANCELLED`.
5. System records the cancellation reason as requested by the Customer.
6. System displays a success message.

**Alternative Flows:**
- **Irregular Status:**
  - If the order is not in a `PENDING` state, the system rejects the cancellation and informs the user that the order cannot be canceled.

**Postconditions:**
- The order status is permanently changed to `CANCELLED`.
- The order can no longer be confirmed or modified.

**Business Rules:**
- **BR1:** A Customer can only cancel their own orders.
- **BR2:** Orders can ONLY be cancelled if their current status is `PENDING`.
- **BR3:** Once cancelled, an order is immutable and cannot be confirmed.

---

## UC-09: Forgot Password

**Actors:** Customer  
**Description:** Allows a Customer to reset their password via OTP when forgotten. The system validates the OTP and ensures all active sessions are invalidated upon resetting for security.

**Preconditions:**
- Customer has an existing account with a registered email address.

**Main Flow:**
1. Customer accesses the Forgot Password page and submits their registered email.
2. System verifies the email exists.
3. System enforces a cooldown period to prevent spam.
4. System generates a 6-digit OTP, stores its generation timestamp, and emails it to the Customer.
5. Customer navigates to the Reset Password page.
6. Customer inputs the OTP, the new password, and confirms the new password.
7. System verifies the OTP matches and has not expired (must be within 5 minutes).
8. System verifies the new password matches the confirmation and follows security policies.
9. System securely hashes and updates the password in the database.
10. System invalidates all existing active sessions for this user across all devices.
11. System redirects the Customer to the Login page with a success message.

**Alternative Flows:**
- **Resend OTP:**
  - Customer requests a new OTP.
  - System checks the escalating cooldown timer (30s, 60s, 120s, etc.).
  - If allowed, a new OTP is sent. If not, an error message is displayed.

**Postconditions:**
- The Customer's password is successfully updated.
- **Security Requirement:** All previous active sessions are explicitly invalidated to prevent unauthorized access from formerly authenticated sessions.

**Business Rules:**
- **BR1:** OTPs have a strict expiration time of 5 minutes (300 seconds).
- **BR2:** The system must enforce an escalating cooldown time between OTP requests.
- **BR3:** The new password must follow standard security requirements (8-12 chars, upper, lower, number, special char).
- **BR4:** Upon successful password reset, the system must immediately invalidate all currently active sessions for the user.

---

## UC-10: Edit Profile

**Actors:** Customer  
**Description:** Allows a Customer to securely update their personal profile information such as Full Name, Phone Number, Date of Birth, Gender, and Address.

**Preconditions:**
- Customer is authenticated and has an active profile.

**Main Flow:**
1. Customer accesses the Edit Profile page.
2. System loads and displays the Customer's existing information.
3. Customer modifies the allowed fields (Full Name, Phone, Address, Date of Birth, Gender) and submits the form.
4. System validates that the Phone Number format is correct (10 digits starting with 0).
5. System validates that the Date of Birth is within the acceptable range (1950 to current date).
6. System validates that the Full Name is not null, empty, or consisting only of whitespace.
7. System validates that the Address is not empty.
8. System updates the information inside both the Account and Customer database records.
9. System displays a success message.

**Alternative Flows:**
- **Empty or Invalid Input:**
  - If validation fails on any field, the system rejects the update and redirects the user back with specific error messages.

**Postconditions:**
- The Customer's profile constraints and details are successfully updated.

**Business Rules:**
- **BR1:** Date of birth must not be a future date and must be after the year 1950.
- **BR2:** Phone number must be exactly 10 digits and start with 0.
- **BR3:** **Security Validation:** `fullName` must be validated on the backend and cannot be null, empty, or whitespace only.
- **BR4:** Address must not be empty.
- **BR5:** Email addresses are immutable and cannot be altered through this use case.
