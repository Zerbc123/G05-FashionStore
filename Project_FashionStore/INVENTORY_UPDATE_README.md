# Cập nhật chức năng Quản Lý Kho - Sử dụng dữ liệu thật từ database

## Những thay đổi đã thực hiện:

### 1. Tạo InventoryService mới
- **File**: `src/main/java/vn/edu/fpt/fashionstore/service/InventoryService.java`
- **Chức năng**:
  - `getAllProductVariants()`: Lấy tất cả variants từ database
  - `getAllProductVariants(Pageable pageable)`: Lấy variants với pagination
  - `getProductVariantsByCategory(String category)`: Lọc theo danh mục
  - `getProductVariantsByCategory(String category, Pageable pageable)`: Lọc theo danh mục với pagination
  - `getProductVariantsByStockStatus(String stockStatus)`: Lọc theo trạng thái tồn kho
  - `getProductVariantsByStockStatus(String stockStatus, Pageable pageable)`: Lọc theo trạng thái tồn kho với pagination
  - `searchProductVariants(String searchTerm)`: Tìm kiếm theo tên sản phẩm
  - `searchProductVariants(String searchTerm, Pageable pageable)`: Tìm kiếm với pagination
  - `updateStock(Integer variantId, Integer newStock)`: Cập nhật số lượng tồn kho
  - `getInventoryStats()`: Lấy thống kê tổng quan

### 2. Cập nhật StaffController
- **File**: `src/main/java/vn/edu/fpt/fashionstore/controller/StaffController.java`
- **Các thay đổi**:
  - Thêm `InventoryService` injection
  - Cập nhật method `inventory()` để hỗ trợ pagination parameters (page, size)
  - Thêm POST endpoint `/inventory/update-stock` để cập nhật tồn kho
  - Thêm GET endpoint `/inventory/filter` để hỗ trợ tìm kiếm, lọc và pagination

### 3. Cập nhật template inventory.html
- **File**: `src/main/resources/templates/staff/inventory.html`
- **Các thay đổi**:
  - Thay thế dữ liệu hardcode bằng Thymeleaf expressions
  - Hiển thị thống kê thật từ database
  - Hiển thị danh sách sản phẩm variants từ database với pagination
  - Form cập nhật tồn kho hoạt động với backend
  - **Pagination functionality**:
    - Dynamic page numbers với active state
    - Previous/Next buttons với disabled states
    - Page size selector (5, 10, 20, 50 items/trang)
    - Page info showing current range and total items
  - **Enhanced filtering**:
    - Tìm kiếm theo tên sản phẩm
    - Lọc theo danh mục (Áo, Quần, Phụ kiện)
    - Lọc theo trạng thái tồn kho (Còn hàng, Sắp hết, Hết hàng)
    - Preserve filter parameters khi pagination
    - Reset to page 0 khi apply filters

## Cách hoạt động:

### 1. Hiển thị dữ liệu
- Thống kê (Tổng sản phẩm, Còn hàng, Sắp hết, Hết hàng) được tính từ real data
- Danh sách sản phẩm hiển thị từ bảng `ProductVariant` với pagination
- Mỗi variant hiển thị: hình ảnh, tên sản phẩm, danh mục, giá, tồn kho, trạng thái

### 2. Pagination
- **Default**: 10 items/trang
- **Page size options**: 5, 10, 20, 50 items/trang
- **Page navigation**: Previous/Next buttons + numbered pages
- **Smart pagination**: Preserve filters khi đổi trang
- **Page info**: Hiển thị "X-Y trong Z sản phẩm"

### 3. Cập nhật tồn kho
- Staff có thể nhập số lượng mới và click "Cập nhật"
- Form submit đến endpoint `/staff/inventory/update-stock`
- Backend cập nhật database và trả về success/error message

### 4. Tìm kiếm và Lọc
- **Tìm kiếm**: Theo tên sản phẩm hoặc description
- **Lọc theo danh mục**: Áo, Quần, Phụ kiện
- **Lọc theo trạng thái**: Còn hàng (>20), Sắp hết (1-20), Hết hàng (0)
- **Filter persistence**: Parameters được giữ khi pagination
- Support Enter key để tìm kiếm

## URL Parameters:
- `page`: Trang hiện tại (default: 0)
- `size`: Số items/trang (default: 10)
- `search`: Từ khóa tìm kiếm
- `category`: Danh mục lọc
- `stockStatus`: Trạng thái tồn kho

## Quy tắc tồn kho:
- **Còn hàng**: stock > 20
- **Sắp hết**: 1 <= stock <= 20  
- **Hết hàng**: stock = 0 hoặc null

## Database schema được sử dụng:
- `ProductVariant` table (variant_id, product_id, color_id, category_size_id, price, stock, image_url)
- `Product` table (product_id, product_name, description, category_id, account_id)
- `Category` table (category_id, category_name)

## Testing:
1. Login với tài khoản staff có role "Quản lý kho (Stock)" (role_id = 4)
2. Navigate to `/staff/inventory`
3. Kiểm tra:
   - Thống kê hiển thị đúng số lượng
   - Danh sách sản phẩm hiển thị từ database với pagination
   - Page size selector hoạt động
   - Pagination navigation hoạt động
   - Filter parameters được preserve khi đổi trang
   - Cập nhật tồn kho hoạt động
   - Tìm kiếm và lọc hoạt động

## Performance considerations:
- Pagination giúp reduce database load cho large datasets
- Client-side filtering cho better UX
- Server-side pagination cho scalability

## Lưu ý:
- Functionality đã được implement nhưng cần test với real database
- Có thể cần thêm validation cho stock input (không âm, max value)
- Pagination hiện tại load all records rồi filter - có thể optimize với database-level filtering
- Image URLs từ database hoặc fallback image nếu null
