package vn.edu.fpt.fashionstore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.fashionstore.entity.*;
import vn.edu.fpt.fashionstore.repository.*;
import vn.edu.fpt.fashionstore.service.CartService;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final AccountRepository accountRepository;
    private final ProductVariantRepository productVariantRepository;
    private final VoucherRepository voucherRepository;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            CartService cartService,
            AccountRepository accountRepository,
            ProductVariantRepository productVariantRepository,
            VoucherRepository voucherRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartService = cartService;
        this.accountRepository = accountRepository;
        this.productVariantRepository = productVariantRepository;
        this.voucherRepository = voucherRepository;
    }

    // =======================================================
    // 1. TẠO ĐƠN HÀNG TỪ GIỎ HÀNG
    // =======================================================
    @Transactional
    public Order createOrderFromCart(Customer customer, String deliveryAddress) {
        try {
            // Kiểm tra customer null
            if (customer == null) {
                throw new RuntimeException("Customer không được để trống!");
            }
            
            // Kiểm tra deliveryAddress
            if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
                throw new RuntimeException("Địa chỉ giao hàng không được để trống!");
            }
            
            // Lấy các sản phẩm trong giỏ hàng
            List<CartItem> cartItems = cartService.getCartItems(customer);
            
            if (cartItems.isEmpty()) {
                throw new RuntimeException("Giỏ hàng trống, không thể tạo đơn hàng!");
            }

            // Tính tổng tiền đơn hàng từ các sản phẩm trong đơn
            double totalAmount = calculateOrderTotalAmount(cartItems);

            // Tạo đơn hàng mới với địa chỉ giao hàng và tài khoản
            Order order = new Order(customer, totalAmount);
            order.setShippingAddress(deliveryAddress);
            if (customer.getAccount() != null) {
                order.setAccount(customer.getAccount());
            }
            order = orderRepository.save(order);
            
            // Lưu OrderItem vào database
            List<OrderItem> orderItems = new ArrayList<>();
            for (CartItem cartItem : cartItems) {
                OrderItem orderItem = new OrderItem(order, cartItem.getProductVariant(), cartItem.getQuantity());
                orderItem = orderItemRepository.save(orderItem);
                orderItems.add(orderItem);
            }
            
            // Clear cart sau khi đã lưu OrderItem thành công
            cartService.clearCart(customer);

            return order;

        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo đơn hàng: " + e.getMessage(), e);
        }
    }

    // 1b. TẠO ĐƠN HÀNG TỪ MUA NGAY (KHÔNG XÓA GIỎ HÀNG)
    // =======================================================
    @Transactional
    public Order createOrderFromBuyNowItems(Customer customer, String deliveryAddress, List<CartItem> buyNowItems) {
        try {
            // Kiểm tra customer null
            if (customer == null) {
                throw new RuntimeException("Customer không được để trống!");
            }
            
            // Kiểm tra deliveryAddress
            if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
                throw new RuntimeException("Địa chỉ giao hàng không được để trống!");
            }
            
            if (buyNowItems == null || buyNowItems.isEmpty()) {
                throw new RuntimeException("Không có sản phẩm để mua!");
            }

            // Tính tổng tiền đơn hàng từ các sản phẩm mua ngay
            double totalAmount = calculateOrderTotalAmount(buyNowItems);

            // Tạo đơn hàng mới với địa chỉ giao hàng và tài khoản
            Order order = new Order(customer, totalAmount);
            order.setShippingAddress(deliveryAddress);
            if (customer.getAccount() != null) {
                order.setAccount(customer.getAccount());
            }
            order = orderRepository.save(order);
            
            // Lưu OrderItem vào database
            List<OrderItem> orderItems = new ArrayList<>();
            for (CartItem cartItem : buyNowItems) {
                OrderItem orderItem = new OrderItem(order, cartItem.getProductVariant(), cartItem.getQuantity());
                orderItem = orderItemRepository.save(orderItem);
                orderItems.add(orderItem);
            }
            
            // KHÔNG clear cart cho MUA NGAY - giữ nguyên giỏ hàng cũ

            return order;

        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo đơn hàng: " + e.getMessage(), e);
        }
    }

    // =======================================================
    // 2. LẤY THÔNG TIN ĐƠN HÀNG
    // =======================================================
    @Transactional(readOnly = true)
    public Order getOrderById(Long orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy đơn hàng với ID: " + orderId);
        }
        return orderOpt.get();
    }

    // Method này không còn dùng vì database không có order_code
    // Có thể dùng getOrderById thay thế
    @Deprecated
    @Transactional(readOnly = true)
    public Order getOrderByCode(String orderCode) {
        // Nếu orderCode có dạng #123456, lấy số
        if (orderCode.startsWith("#")) {
            orderCode = orderCode.substring(1);
        }
        
        try {
            Long orderId = Long.parseLong(orderCode);
            return getOrderById(orderId);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Mã đơn hàng không hợp lệ: " + orderCode);
        }
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomer(Customer customer) {
        if (customer == null) {
            return Collections.emptyList();
        }
        return orderRepository.findByCustomerOrderByOrderDateDesc(customer);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomerAndStatus(Customer customer, OrderStatus status) {
        if (customer == null || status == null) {
            return Collections.emptyList();
        }
        return orderRepository.findByCustomerAndStatusOrderByOrderDateDesc(customer, status);
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        // Sort manually by orderDate descending
        orders.sort((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate()));
        return orders;
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatusOrderByOrderDateDesc(status);
    }

    // =======================================================
    // 3. CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG
    // =======================================================
    @Transactional
    public Order confirmOrder(Long orderId, String confirmedBy) {
        Order order = getOrderById(orderId);
        
        if (!order.canBeConfirmed()) {
            throw new RuntimeException("Đơn hàng này không thể xác nhận! Trạng thái hiện tại: " + order.getStatus().getDisplayName());
        }
        
        order.confirm(confirmedBy);
        order = orderRepository.save(order);
        
        System.out.println("[ORDER SERVICE] Order " + order.getOrderCode() + " confirmed by: " + confirmedBy);
        return order;
    }

    @Transactional
    public Order cancelOrder(Long orderId, String cancelledBy, String reason) {
        Order order = getOrderById(orderId);
        
        if (!order.canBeCancelled()) {
            throw new RuntimeException("Đơn hàng này không thể hủy! Trạng thái hiện tại: " + order.getStatus().getDisplayName());
        }
        
        order.cancel(cancelledBy, reason);
        order = orderRepository.save(order);
        
        System.out.println("[ORDER SERVICE] Order " + order.getOrderCode() + " cancelled by: " + cancelledBy + ", reason: " + reason);
        return order;
    }

    // =======================================================
    // 4. KIỂM TRA QUYỀN SỞ HỮU ĐƠN HÀNG
    // =======================================================
    @Transactional(readOnly = true)
    public boolean isOrderOwnedByCustomer(Long orderId, Customer customer) {
        return orderRepository.existsByOrderIdAndCustomer(orderId, customer);
    }

    // =======================================================
    // 5. THỐNG KÊ VÀ BÁO CÁO
    // =======================================================
    @Transactional(readOnly = true)
    public long countOrdersByStatus(OrderStatus status) {
        return orderRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public long countCustomerOrders(Customer customer) {
        return orderRepository.countByCustomer(customer);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByDateRange(Date startDate, Date endDate) {
        return orderRepository.findByOrderDateBetween(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public Double calculateRevenueBetween(Date startDate, Date endDate) {
        return orderItemRepository.calculateRevenueBetween(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getTopSellingProducts() {
        return orderItemRepository.findTopSellingVariants();
    }

    // =======================================================
    // 6. HELPER METHODS
    // =======================================================
    private double calculateItemTotal(CartItem cartItem) {
        return cartItem.getProductVariant().getPrice() * cartItem.getQuantity();
    }

    private double calculateOrderTotalAmount(List<CartItem> cartItems) {
        return cartItems.stream()
                .mapToDouble(this::calculateItemTotal)
                .sum();
    }

    private String generateOrderCode() {
        // Tạo mã đơn hàng theo format: ORD + timestamp + random
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 1000);
        return "ORD" + timestamp + random;
    }

    // Lấy OrderItems từ database
    @Transactional(readOnly = true)
    public List<OrderItem> getOrderItemsByOrder(Order order) {
        System.out.println("[ORDER SERVICE] getOrderItemsByOrder called for Order ID: " + order.getOrderId());
        List<OrderItem> orderItems = orderItemRepository.findByOrderOrderByOrderItemIdAsc(order);
        System.out.println("[ORDER SERVICE] Found " + orderItems.size() + " OrderItems");
        return orderItems;
    }

    // TẠM THỜI: Dùng totalAmount từ order vì không có OrderItem
    @Transactional(readOnly = true)
    public Double calculateOrderTotal(Order order) {
        return order.getTotalAmount();
    }

    // =======================================================
    // 7. VALIDATION METHODS
    // =======================================================
    @Transactional(readOnly = true)
    public boolean canCustomerCancelOrder(Long orderId, Customer customer) {
        try {
            Order order = getOrderById(orderId);
            return isOrderOwnedByCustomer(orderId, customer) && order.canBeCancelled();
        } catch (Exception e) {
            return false;
        }
    }

    // =======================================================
    // 8. SESSION & HELPER METHODS (CHUYỂN TỪ CONTROLLER)
    // =======================================================
    
    @Transactional(readOnly = true)
    public Customer getCurrentCustomer(HttpSession session) {
        try {
            String email = (String) session.getAttribute("user");
            if (email == null) {
                System.out.println("No user in session");
                throw new RuntimeException("Bạn chưa đăng nhập!");
            }
            
            System.out.println("Finding customer for email in OrderService: " + email);
            
            Optional<Account> accountOpt = accountRepository.findByEmail(email);
            if (accountOpt.isEmpty()) {
                System.out.println("Account not found for email: " + email);
                throw new RuntimeException("Không tìm thấy tài khoản!");
            }
            
            Account account = accountOpt.get();
            System.out.println("Found account: " + account.getAccountId());
            
            if (account.getCustomers() == null || account.getCustomers().isEmpty()) {
                System.out.println("No customers found for account: " + account.getAccountId());
                throw new RuntimeException("Không tìm thấy thông tin khách hàng!");
            }
            
            Customer customer = account.getCustomers().get(0);
            System.out.println("Found customer: " + customer.getCustomerId());
            
            return customer;
        } catch (Exception e) {
            System.err.println("Error in OrderService.getCurrentCustomer: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi lấy thông tin khách hàng: " + e.getMessage(), e);
        }
    }
    
    @Transactional(readOnly = true)
    public List<CartItem> getCartItemsForCheckout(HttpSession session, Customer currentCustomer) {
        List<CartItem> cartItems;
        
        // Kiểm tra xem đây là MUA NGAY hay checkout giỏ hàng thường
        Boolean isBuyNow = (Boolean) session.getAttribute("isBuyNow");
        if (isBuyNow != null && isBuyNow) {
            // MUA NGAY - chỉ lấy sản phẩm được chọn
            System.out.println("BUY NOW MODE - getting selected product only");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> buyNowItems = (List<Map<String, Object>>) session.getAttribute("buyNowItems");
            
            if (buyNowItems == null || buyNowItems.isEmpty()) {
                throw new RuntimeException("Không tìm thấy sản phẩm được chọn!");
            }
            
            // Tạo cartItems tạm thời từ buyNowItems
            cartItems = new ArrayList<>();
            for (Map<String, Object> item : buyNowItems) {
                Integer variantId = (Integer) item.get("variantId");
                Integer quantity = (Integer) item.get("quantity");
                
                // Tìm ProductVariant
                ProductVariant variant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm variant"));
                
                // Tạo CartItem tạm thời
                CartItem tempCartItem = new CartItem();
                tempCartItem.setProductVariant(variant);
                tempCartItem.setQuantity(quantity);
                tempCartItem.setCustomer(currentCustomer);
                
                cartItems.add(tempCartItem);
            }
            
            // Xóa session attributes sau khi sử dụng
            session.removeAttribute("buyNowItems");
            session.removeAttribute("isBuyNow");
            
        } else {
            // CHECKOUT THƯỜNG - lấy toàn bộ giỏ hàng
            System.out.println("NORMAL CHECKOUT - getting all cart items");
            cartItems = cartService.getCartItems(currentCustomer);
        }
        
        return cartItems;
    }
    
    @Transactional(readOnly = true)
    public String getDeliveryAddressFromSession(HttpSession session, Customer currentCustomer) {
        // Ưu tiên địa chỉ từ session (đã chọn từ trang chọn địa chỉ), nếu không có thì dùng từ profile
        String deliveryAddress = (String) session.getAttribute("deliveryAddress");
        if (deliveryAddress != null && !deliveryAddress.trim().isEmpty()) {
            return deliveryAddress;
        } else {
            return currentCustomer.getAddress();
        }
    }
    
    @Transactional(readOnly = true)
    public boolean validateCheckoutData(String fullName, String phone, String deliveryAddress) {
        // Bắt lỗi: Tên rỗng hoặc chứa số/ký tự đặc biệt
        if (fullName.trim().isEmpty()) {
            throw new RuntimeException("Vui lòng nhập họ và tên của bạn.");
        } else if (!fullName.matches("^[\\p{L}\\s]+$")) {
            // Regex: \p{L} là chữ cái bất kỳ (hỗ trợ tiếng Việt), \s là khoảng trắng
            throw new RuntimeException("Họ và tên chỉ được chứa chữ cái, không nhập số hay ký tự đặc biệt.");
        }

        // Bắt lỗi: Số điện thoại
        if (phone.trim().isEmpty() || !phone.matches("^0[0-9]{9}$")) {
            throw new RuntimeException("Số điện thoại không hợp lệ (Bắt buộc 10 số và bắt đầu bằng 0).");
        }

        // Bắt lỗi: Địa chỉ giao hàng
        if (deliveryAddress.trim().isEmpty()) {
            throw new RuntimeException("Vui lòng chọn địa chỉ giao hàng.");
        }
        
        return true;
    }
    
    @Transactional(readOnly = true)
    public List<CartItem> getCartItemsForValidation(HttpSession session, Customer currentCustomer) {
        // Kiểm tra xem đây là MUA NGAY hay checkout giỏ hàng thường
        Boolean isBuyNow = (Boolean) session.getAttribute("isBuyNow");
        if (isBuyNow != null && isBuyNow) {
            // MUA NGAY - chỉ lấy sản phẩm được chọn
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> buyNowItems = (List<Map<String, Object>>) session.getAttribute("buyNowItems");
            
            List<CartItem> cartItems = new ArrayList<>();
            for (Map<String, Object> item : buyNowItems) {
                Integer variantId = (Integer) item.get("variantId");
                Integer quantity = (Integer) item.get("quantity");
                
                ProductVariant variant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm variant"));
                
                CartItem tempCartItem = new CartItem();
                tempCartItem.setProductVariant(variant);
                tempCartItem.setQuantity(quantity);
                tempCartItem.setCustomer(currentCustomer);
                
                cartItems.add(tempCartItem);
            }
            return cartItems;
        } else {
            // CHECKOUT THƯỜNG - lấy toàn bộ giỏ hàng
            return cartService.getCartItems(currentCustomer);
        }
    }
    
    @Transactional
    public Order createOrderFromSessionData(HttpSession session, Customer currentCustomer, String deliveryAddress) {
        // Kiểm tra xem đây là MUA NGAY hay checkout giỏ hàng thường
        Boolean isBuyNow = (Boolean) session.getAttribute("isBuyNow");
        if (isBuyNow != null && isBuyNow) {
            // MUA NGAY - chỉ lấy sản phẩm được chọn
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> buyNowItems = (List<Map<String, Object>>) session.getAttribute("buyNowItems");
            
            List<CartItem> buyNowCartItems = new ArrayList<>();
            for (Map<String, Object> item : buyNowItems) {
                Integer variantId = (Integer) item.get("variantId");
                Integer quantity = (Integer) item.get("quantity");
                
                ProductVariant variant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm variant"));
                
                CartItem tempCartItem = new CartItem();
                tempCartItem.setProductVariant(variant);
                tempCartItem.setQuantity(quantity);
                tempCartItem.setCustomer(currentCustomer);
                
                buyNowCartItems.add(tempCartItem);
            }
            
            // Tạo đơn hàng từ MUA NGAY items (KHÔNG clear cart)
            Order order = createOrderFromBuyNowItems(currentCustomer, deliveryAddress, buyNowCartItems);
            
            // Xóa session attributes sau khi sử dụng
            session.removeAttribute("buyNowItems");
            session.removeAttribute("isBuyNow");
            
            return order;
        } else {
            // CHECKOUT THƯỜNG - lấy toàn bộ giỏ hàng
            return createOrderFromCart(currentCustomer, deliveryAddress);
        }
    }
    
    @Transactional(readOnly = true)
    public List<Voucher> getValidVouchers() {
        return voucherRepository.findByIsActiveTrueAndExpiredDateGreaterThanEqual(new java.util.Date());
    }
    
    @Transactional(readOnly = true)
    public double calculateCartTotal(List<CartItem> cartItems) {
        return cartService.getCartTotal(cartItems);
    }
    
    @Transactional
    public Order createOrderFromSessionDataWithTotal(HttpSession session, Customer currentCustomer, String deliveryAddress, Double totalAmount) {
        // Tạo đơn hàng từ session data
        Order order = createOrderFromSessionData(session, currentCustomer, deliveryAddress);
        
        // Cập nhật tổng tiền đơn hàng với giá trị từ frontend
        order.setTotalAmount(totalAmount);
        
        // Lưu Order vào database
        order = orderRepository.save(order);
        
        return order;
    }
    
    @Transactional
    public void updateDeliveryAddressInSession(HttpSession session, Customer currentCustomer, String addressOption, String finalAddress) {
        // Lưu địa chỉ vào session để sử dụng khi đặt hàng
        if ("profile".equals(addressOption)) {
            // Sử dụng địa chỉ từ profile
            session.setAttribute("deliveryAddress", currentCustomer.getAddress());
        } else if ("new".equals(addressOption) && finalAddress != null && !finalAddress.trim().isEmpty()) {
            // Sử dụng địa chỉ mới cho đơn hàng này
            session.setAttribute("deliveryAddress", finalAddress);
        } else {
            throw new RuntimeException("Vui lòng chọn địa chỉ giao hàng!");
        }
    }



}
