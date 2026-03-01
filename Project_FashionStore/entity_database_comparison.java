// BẢNG SO SÁNH ENTITY VÀ DATABASE
// Dùng kết quả từ SQL để so sánh với entity Java

// =======================================================
// 1. ENTITY ORDER HIỆN TẠI
// =======================================================
@Entity
@Table(name = "orders")  // Cần kiểm tra tên table
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")  // Cần kiểm tra tên column
    private Long orderId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")  // Cần kiểm tra tên column
    private Customer customer;
    
    @Column(name = "total_amount")  // Cần kiểm tra tên column
    private Double totalAmount;
    
    @Column(name = "order_date")  // Cần kiểm tra tên column
    private Date orderDate;
    
    @Column(name = "status")  // Cần kiểm tra tên column
    private String status;
}

// =======================================================
// 2. ENTITY ORDERITEM HIỆN TẠI
// =======================================================
@Entity
@Table(name = "orderitem")  // Cần kiểm tra tên table
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")  // Cần kiểm tra tên column
    private Long orderItemId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")  // Cần kiểm tra tên column
    private Order order;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")  // Cần kiểm tra tên column
    private ProductVariant productVariant;
    
    @Column(name = "quantity")  // Cần kiểm tra tên column
    private Integer quantity;
    
    @Column(name = "unitprice")  // ĐÃ SỬA - Cần kiểm tra
    private Double unitPrice;
    
    @Column(name = "price")  // ĐÃ SỬA - Cần kiểm tra
    private Double totalPrice;
}

// =======================================================
// 3. ENTITY CART HIỆN TẠI
// =======================================================
@Entity
@Table(name = "cart")  // Cần kiểm tra tên table
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")  // Cần kiểm tra tên column
    private Long cartId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")  // Cần kiểm tra tên column
    private Customer customer;
}

// =======================================================
// 4. ENTITY CARTITEM HIỆN TẠI
// =======================================================
@Entity
@Table(name = "cartitem")  // Cần kiểm tra tên table
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")  // Cần kiểm tra tên column
    private Long cartItemId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")  // Cần kiểm tra tên column
    private Cart cart;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")  // Cần kiểm tra tên column
    private ProductVariant productVariant;
    
    @Column(name = "quantity")  // Cần kiểm tra tên column
    private Integer quantity;
}

// =======================================================
// 5. ENTITY PRODUCTVARIANT HIỆN TẠI
// =======================================================
@Entity
@Table(name = "productvariant")  // Cần kiểm tra tên table
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id")  // Cần kiểm tra tên column
    private Long variantId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")  // Cần kiểm tra tên column
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "size_id")  // Cần kiểm tra tên column
    private CategorySize categorySize;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id")  // Cần kiểm tra tên column
    private Color color;
    
    @Column(name = "price")  // Cần kiểm tra tên column
    private Double price;
    
    @Column(name = "image_url")  // Cần kiểm tra tên column
    private String imageUrl;
}

// =======================================================
// 6. CÁC VẤN ĐỀ CẦN KIỂM TRA
// =======================================================
/*
1. TÊN BẢNG:
   - orders hay order?
   - orderitem hay order_items?
   - cart hay carts?
   - cartitem hay cart_items?
   - productvariant hay product_variants?

2. TÊN COLUMN:
   - order_id hay orderid?
   - customer_id hay customerid?
   - total_amount hay totalamount?
   - order_date hay orderdate?
   - unit_price hay unitprice?
   - total_price hay totalamount hay price?
   - image_url hay imageurl?

3. DATA TYPES:
   - INT hay BIGINT cho ID?
   - FLOAT hay DECIMAL cho price?
   - VARCHAR hay NVARCHAR cho text?
   - DATETIME hay TIMESTAMP cho date?

4. NULLABLE:
   - Các column bắt buộc phải có NOT NULL
   - Các column có thể null mới được nullable = true

5. FOREIGN KEY:
   - Tên column foreign key có khớp?
   - Referenced table có tồn tại?
*/

// =======================================================
// 7. HƯỚNG DẪN SỬA LỖI
// =======================================================
/*
Bước 1: Chạy check_all_tables_complete.sql
Bước 2: So sánh kết quả với entity ở trên
Bước 3: Sửa lại @Table và @Column cho khớp
Bước 4: Restart server và test lại
*/
