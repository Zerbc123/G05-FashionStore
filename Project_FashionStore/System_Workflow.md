# Hướng Dẫn Phân Tích Luồng Hệ Thống (Prompt Template)

Dưới đây là cấu trúc prompt tuân thủ nghiêm ngặt các yêu cầu phân tích sâu sắc, chất lượng cao và tuyệt đối không thay đổi mã nguồn hiện có của dự án. 

```markdown
Đóng vai là một Chuyên gia Phân tích Hệ thống (System Analyst) và Kiến trúc sư Phần mềm (Software Architect). Nhiệm vụ của bạn là đọc toàn bộ mã nguồn của dự án theo kiến trúc MVC/Spring Boot (cụ thể là các Entity, Controller, Service, Repository) để xác định danh sách các luồng nghiệp vụ chính (Main Workflows) và luồng nghiệp vụ phụ (Sub-workflows) của ứng dụng Ecommerce Fashion Store.

YÊU CẦU CHẤT LƯỢNG MÀ BẠN PHẢI TUÂN THỦ:
1. Phân tích sâu sắc, logic và sát với thực tế mã nguồn (dựa trên các class thực tế như Order, CartItem, Payment, Wishlist, Account, v.v.).
2. Không bỏ sót các tính năng quan trọng (như Thanh toán Momo, Xác thực OAuth2, Gửi Email).
3. Trình bày rõ ràng, phân nhóm theo từng Actor (Customer, Admin/Staff).
4. Phân tách rõ ràng giữa Main Flow và Sub-Flow.
5. Tuyệt đối không được thay đổi, chỉnh sửa hay xóa bất kỳ dòng code nào trong dự án, chỉ tập trung vào phân tích thiết kế và nghiệp vụ để lập tài liệu.
```

---

# Phân Tích Luồng Hệ Thống (System Workflows)

Dựa vào việc thực thi prompt phân tích trên với mã nguồn hiện tại của dự án (`Project_FashionStore`), dưới đây là kết quả phân tích và bóc tách các luồng trong hệ thống thực tế.

## 1. Các Luồng Nghiệp Vụ Chính (Main Workflows)

### 1.1. Luồng Quản lý Tài khoản & Xác thực (Authentication & Authorization Flow)
- **Actor:** Khách hàng (Customer), Quản trị viên (Admin), Nhân viên (Staff).
- **Mô tả:** Dòng chảy cho phép người dùng định danh và cấp quyền truy cập vào các chức năng hệ thống tùy theo vai trò.
- **Các điểm chạm (Touchpoints):** 
  - Đăng nhập hoặc Đăng ký tài khoản bằng email/mật khẩu truyền thống.
  - Đăng nhập/Xác thực qua nền tảng bên thứ 3 (OAuth2 CustomUserService).
  - Phân quyền truy cập các endpoint theo nhóm quyền (Role-based access).
  - Customer cập nhật hồ sơ cá nhân.

### 1.2. Luồng Mua Sắm & Đặt Hàng (Shopping & Checkout Flow)
- **Actor:** Khách hàng (Customer).
- **Mô tả:** Luồng cốt lõi (Core workflow) từ khi khách hàng tìm kiếm đến khi chốt đơn.
- **Các điểm chạm (Touchpoints):**
  - Xem danh sách sản phẩm, chi tiết sản phẩm theo màu sắc, kích cỡ (Product, ProductVariant, Color, CategorySize).
  - Thêm sản phẩm vào giỏ hàng và Cập nhật giỏ hàng (CartItem, CartController).
  - Áp dụng các Mã giảm giá hợp lệ (Voucher).
  - Tiến hành Đặt hàng (Checkout) và lưu thông tin Đơn hàng (Order, OrderItem).
  - Lựa chọn phương thức thanh toán: Tiền mặt (COD) hoặc Trực tuyến (Momo).

### 1.3. Luồng Xử Lý Thanh Toán Điện Tử (Online Payment Processing Flow)
- **Actor:** Hệ thống, Khách hàng, Cổng thanh toán (Momo).
- **Mô tả:** Tích hợp API bên thứ ba để xác nhận giao dịch tài chính.
- **Các điểm chạm (Touchpoints):** 
  - Web tạo request thanh toán và gửi tới Momo (MomoRequestDTO).
  - Hệ thống hứng callback/response từ Momo trả về sau khi giao dịch trên app hoàn tất (MomoResponseDTO, MomoService).
  - Tự động đối soát và cập nhật tình trạng giao dịch của Đơn hàng mới lập.

### 1.4. Luồng Quản Lý Đơn Hàng (Order Management Flow)
- **Actor:** Nhân viên (Staff), Quản trị viên (Admin).
- **Mô tả:** Quản lý vòng đời đơn hàng tiếp nhận từ khách.
- **Các điểm chạm (Touchpoints):**
  - Hiển thị danh sách và chi tiết các đơn hàng theo trạng thái.
  - Cập nhật tuần tự trạng thái đơn (OrderStatus): Chờ xác nhận -> Đang xử lý -> Đang giao -> Hoàn thành.
  - Thực hiện Hủy đơn với lý do hoặc Xác nhận đổi trả.

### 1.5. Luồng Quản Lý Kho & Sản Phẩm (Product & Inventory Management Flow)
- **Actor:** Quản trị viên (Admin), Nhân viên (Staff).
- **Mô tả:** Quản lý dữ liệu thư viện sản phẩm (Catalog) phục vụ cho cửa hàng.
- **Các điểm chạm (Touchpoints):**
  - Quản lý cấu trúc danh mục kinh doanh (Category).
  - Tạo mới, cập nhật, vô hiệu hóa sản phẩm (Product).
  - Quản lý chi tiết các biến thể mã hàng tồn kho (SKU/ProductVariant) đi kèm Kích cỡ và Màu sắc.

---

## 2. Các Luồng Nghiệp Vụ Phụ (Sub-workflows)

### 2.1. Luồng Quản Lý Sản Phẩm Yêu Thích (Wishlist Flow)
- **Actor:** Khách hàng (Customer).
- **Mô tả:** Cho phép khách hàng "Lưu để xem sau".
- **Các điểm chạm:** Thêm một sản phẩm vào Wishlist, xem danh sách sản phẩm yêu thích, gỡ bỏ khỏi danh sách.

### 2.2. Luồng Sổ Địa Chỉ Giao Hàng (Address Management Flow)
- **Actor:** Khách hàng (Customer).
- **Mô tả:** Quản lý dữ liệu thông tin nhận hàng hỗ trợ quy trình Checkout mượt mà hơn.
- **Các điểm chạm:** Thêm địa chỉ mới, thay đổi địa chỉ mặc định, hoặc chỉnh sửa/xóa địa chỉ cũ (AddressApiController).

### 2.3. Luồng Thông Báo Tự Động (Email Notification Flow)
- **Actor:** Hệ thống.
- **Mô tả:** Tác vụ chạy ngầm hỗ trợ thông tin cho người dùng kịp thời ở các mốc quan trọng.
- **Các điểm chạm:** Gửi email OTP/xác nhận tài khoản, Gửi biên lai/hoá đơn điện tử khi đặt hàng thành công (EmailService).

### 2.4. Luồng Vận Hành Khuyến Mãi (Voucher Flow)
- **Actor:** Quản trị viên (Admin).
- **Mô tả:** Hỗ trợ thúc đẩy kinh doanh thông qua mã giảm giá.
- **Các điểm chạm:** Admin tạo các Voucher (theo phần trăm hoặc giá tiền cố định, quy định số lượng và thời hạn sử dụng).
