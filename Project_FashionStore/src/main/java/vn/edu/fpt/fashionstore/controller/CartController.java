package vn.edu.fpt.fashionstore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.entity.CartItem;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.entity.Order;
import vn.edu.fpt.fashionstore.entity.OrderItem;
import vn.edu.fpt.fashionstore.repository.AccountRepository;
import vn.edu.fpt.fashionstore.repository.CustomerRepository;
import vn.edu.fpt.fashionstore.service.CartService;
import vn.edu.fpt.fashionstore.service.OrderService;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private AccountRepository accountRepository;

    // Lấy customer từ session
    private Customer getCurrentCustomer(HttpSession session) {
        String email = (String) session.getAttribute("user");
        if (email == null) {
            throw new RuntimeException("Bạn chưa đăng nhập!");
        }
        
        Optional<Account> accountOpt = accountRepository.findByEmail(email);
        if (accountOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy tài khoản!");
        }
        
        Account account = accountOpt.get();
        if (account.getCustomers() == null || account.getCustomers().isEmpty()) {
            throw new RuntimeException("Không tìm thấy thông tin khách hàng!");
        }
        
        return account.getCustomers().get(0);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String viewCart(Model model, HttpSession session) {
        try {
            Customer currentCustomer = getCurrentCustomer(session);
            if (currentCustomer == null) {
                return "redirect:/login";
            }
            
            List<CartItem> cartItems = cartService.getCartItems(currentCustomer);
            double total = cartService.getCartTotal(cartItems);

            model.addAttribute("cartItems", cartItems);
            model.addAttribute("total", total);
            
            // Cập nhật số lượng giỏ hàng vào session
            session.setAttribute("cartCount", cartItems != null ? cartItems.size() : 0);
            
            return "cart";
        } catch (RuntimeException e) {
            return "redirect:/login";
        }
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam("productId") Long productId,
                            @RequestParam("sizeId") Integer sizeId,
                            @RequestParam("colorId") Integer colorId,
                            @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        try {
            Customer currentCustomer = getCurrentCustomer(session);
            if (currentCustomer == null) {
                System.out.println("[CART DEBUG] Customer is null, redirecting to login");
                return "redirect:/login";
            }
            
            System.out.println("[CART DEBUG] Adding to cart - ProductID: " + productId + ", SizeID: " + sizeId + ", ColorID: " + colorId + ", Quantity: " + quantity);
            
            // Tìm variant dựa trên productId, sizeId và colorId
            Integer variantId = cartService.findVariantByProductSizeColor(productId, sizeId, colorId);
            
            System.out.println("[CART DEBUG] Found variantId: " + variantId);
            
            if (variantId == null) {
                System.out.println("[CART DEBUG] Variant not found!");
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm với size và màu đã chọn!");
                return "redirect:/products/detail/" + productId;
            }
            
            cartService.addToCart(currentCustomer, variantId, quantity);
            
            // Cập nhật số lượng giỏ hàng
            updateCartCount(session, currentCustomer);
            
            System.out.println("[CART DEBUG] Successfully added to cart!");
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sản phẩm vào giỏ hàng!");
        } catch (RuntimeException e) {
            System.out.println("[CART DEBUG] Error: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/login";
        }
        return "redirect:/cart";
    }

    @PostMapping("/buy-now")
    public String buyNow(@RequestParam("productId") Long productId,
                         @RequestParam("sizeId") Integer sizeId,
                         @RequestParam("colorId") Integer colorId,
                         @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        try {
            Customer currentCustomer = getCurrentCustomer(session);
            if (currentCustomer == null) {
                System.out.println("[BUY NOW DEBUG] Customer is null, redirecting to login");
                return "redirect:/login";
            }
            
            System.out.println("[BUY NOW DEBUG] Buy now - ProductID: " + productId + ", SizeID: " + sizeId + ", ColorID: " + colorId + ", Quantity: " + quantity);
            
            // Tìm variant dựa trên productId, sizeId và colorId
            Integer variantId = cartService.findVariantByProductSizeColor(productId, sizeId, colorId);
            
            System.out.println("[BUY NOW DEBUG] Found variantId: " + variantId);
            
            if (variantId == null) {
                System.out.println("[BUY NOW DEBUG] Variant not found!");
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm với size và màu đã chọn!");
                return "redirect:/products/detail/" + productId;
            }
            
            // Thêm vào giỏ hàng và chuyển thẳng đến checkout
            cartService.addToCart(currentCustomer, variantId, quantity);
            
            // Cập nhật số lượng giỏ hàng
            updateCartCount(session, currentCustomer);
            
            System.out.println("[BUY NOW DEBUG] Successfully added to cart, redirecting to checkout!");
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sản phẩm vào giỏ hàng!");
            return "redirect:/cart/checkout";
            
        } catch (RuntimeException e) {
            System.out.println("[BUY NOW DEBUG] Error: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/login";
        }
    }

    @PostMapping("/update")
    public String updateCart(@RequestParam("cartItemId") Integer cartItemId,
                             @RequestParam("action") String action,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        try {
            // Lấy cart item hiện tại
            CartItem cartItem = cartService.getCartItemById(cartItemId);
            if (cartItem == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm trong giỏ hàng!");
                return "redirect:/cart";
            }
            
            int newQuantity = cartItem.getQuantity();
            if ("increase".equals(action)) {
                newQuantity++;
            } else if ("decrease".equals(action) && newQuantity > 1) {
                newQuantity--;
            }
            
            cartService.updateQuantity(cartItemId, newQuantity);
            
            // Cập nhật số lượng giỏ hàng
            updateCartCount(session, getCurrentCustomer(session));
            
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật giỏ hàng!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String removeFromCart(@RequestParam("cartItemId") Integer cartItemId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        try {
            cartService.removeFromCart(cartItemId);
            
            // Cập nhật số lượng giỏ hàng
            updateCartCount(session, getCurrentCustomer(session));
            
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/cart";
    }
    
    // Helper method để cập nhật số lượng giỏ hàng vào session
    private void updateCartCount(HttpSession session, Customer customer) {
        if (customer == null) {
            session.setAttribute("cartCount", 0);
            return;
        }
        
        List<CartItem> cartItems = cartService.getCartItems(customer);
        session.setAttribute("cartCount", cartItems != null ? cartItems.size() : 0);
    }

    // =======================================================
    // 1. MỞ TRANG CHECKOUT
    // =======================================================
    @GetMapping("/checkout")
    @Transactional(readOnly = true)
    public String checkoutPage(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            Customer currentCustomer = getCurrentCustomer(session);
            if (currentCustomer == null) {
                return "redirect:/login";
            }
            
            List<CartItem> cartItems = cartService.getCartItems(currentCustomer);

            // Nếu giỏ hàng trống thì không cho vào trang checkout
            if (cartItems == null || cartItems.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng của bạn đang trống!");
                return "redirect:/cart";
            }

            double total = cartService.getCartTotal(cartItems);

            // Gửi dữ liệu ra cột bên phải
            model.addAttribute("cartItems", cartItems);
            model.addAttribute("total", total);

            // Lấy sẵn tên và sđt từ Customer điền sẵn vào form cho khách lười gõ
            model.addAttribute("fullName", currentCustomer.getFullName());
            
            // Sử dụng chuỗi điện thoại trực tiếp (đã lưu với số 0 đầu nếu có)
            String phoneStr = currentCustomer.getPhone();
            if (phoneStr == null) phoneStr = "";
            model.addAttribute("phone", phoneStr);
            
            // Ưu tiên địa chỉ từ session (đã chọn từ trang chọn địa chỉ), nếu không có thì dùng từ profile
            String deliveryAddress = (String) session.getAttribute("deliveryAddress");
            if (deliveryAddress != null && !deliveryAddress.trim().isEmpty()) {
                model.addAttribute("address", deliveryAddress);
            } else {
                model.addAttribute("address", currentCustomer.getAddress());
            }

            return "checkout";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/cart";
        }
    }

    // =======================================================
    // 2. XỬ LÝ KHI BẤM NÚT "ĐẶT HÀNG" (MANUAL VALIDATION)
    // =======================================================
    @PostMapping("/checkout/place-order")
    public String placeOrder(
            @RequestParam(value = "fullName", defaultValue = "") String fullName,
            @RequestParam(value = "phone", defaultValue = "") String phone,
            @RequestParam(value = "deliveryAddress", defaultValue = "") String deliveryAddress,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "deliveryMethod", required = false) String deliveryMethod,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        System.out.println("[CART CONTROLLER] placeOrder called with params:");
        System.out.println("  fullName: " + fullName);
        System.out.println("  phone: " + phone);
        System.out.println("  deliveryAddress: " + deliveryAddress);
        System.out.println("  note: " + note);
        System.out.println("  deliveryMethod: " + deliveryMethod);
        System.out.println("  paymentMethod: " + paymentMethod);

        // Lấy customer hiện tại
        Customer currentCustomer = getCurrentCustomer(session);
        
        // Kiểm tra nếu customer không tồn tại
        if (currentCustomer == null) {
            return "redirect:/login";
        }

        boolean hasError = false;

        // Bắt lỗi: Tên rỗng hoặc chứa số/ký tự đặc biệt
        if (fullName.trim().isEmpty()) {
            model.addAttribute("errorFullName", "Vui lòng nhập họ và tên của bạn.");
            hasError = true;
        } else if (!fullName.matches("^[\\p{L}\\s]+$")) {
            // Regex: \p{L} là chữ cái bất kỳ (hỗ trợ tiếng Việt), \s là khoảng trắng
            model.addAttribute("errorFullName", "Họ và tên chỉ được chứa chữ cái, không nhập số hay ký tự đặc biệt.");
            hasError = true;
        }

        // Bắt lỗi: Số điện thoại
        if (phone.trim().isEmpty() || !phone.matches("^0[0-9]{9}$")) {
            model.addAttribute("errorPhone", "Số điện thoại không hợp lệ (Bắt buộc 10 số và bắt đầu bằng 0).");
            hasError = true;
        }

        // Bắt lỗi: Địa chỉ giao hàng
        if (deliveryAddress.trim().isEmpty()) {
            model.addAttribute("errorAddress", "Vui lòng chọn địa chỉ giao hàng.");
            hasError = true;
        }

        // Nếu có lỗi -> Phải Load lại danh sách sản phẩm và trả về trang checkout kèm lỗi
        if (hasError) {
            try {
                List<CartItem> cartItems = cartService.getCartItems(currentCustomer);
                double total = cartService.getCartTotal(cartItems);

                // Gửi lại data giỏ hàng
                model.addAttribute("cartItems", cartItems);
                model.addAttribute("total", total);

                // Giữ nguyên chữ khách đã nhập
                model.addAttribute("fullName", fullName);
                model.addAttribute("phone", phone);
                model.addAttribute("address", deliveryAddress);
                model.addAttribute("note", note);

                return "checkout"; // Trả lại trang để khách sửa lỗi
            } catch (Exception e) {
                return "redirect:/login";
            }
        }
        
        // TẠO ĐƠN HÀNG THỰC TẾ QUA ORDER SERVICE
        try {
            // Sử dụng địa chỉ từ session hoặc từ form
            Order order = orderService.createOrderFromCart(currentCustomer, deliveryAddress);
            
            // Xóa địa chỉ tạm thời khỏi session sau khi đã đặt hàng
            session.removeAttribute("deliveryAddress");
            
            // XÓA CÁC SESSION ATTRIBUTE CÓ THỂ GHI ĐÈ LÊN ORDERID
            session.removeAttribute("orderId");
            session.removeAttribute("productImage");
            session.removeAttribute("productName");
            session.removeAttribute("sizeName");
            session.removeAttribute("colorName");
            
            redirectAttributes.addFlashAttribute("successMessage", "Đặt hàng thành công! Mã đơn hàng của bạn: #" + order.getOrderId());
            
            // Lấy thông tin đơn hàng và trả về template trực tiếp
            model.addAttribute("orderCode", "#" + order.getOrderId());
            model.addAttribute("orderId", order.getOrderId().toString());
            model.addAttribute("orderDate", order.getOrderDate());
            model.addAttribute("deliveryAddress", deliveryAddress); // Sử dụng địa chỉ đã chọn
            model.addAttribute("orderStatus", order.getStatus());
            model.addAttribute("totalAmount", order.getTotalAmount());
            model.addAttribute("customerName", currentCustomer.getFullName());
            model.addAttribute("customerPhone", currentCustomer.getPhone());
            model.addAttribute("customerEmail", currentCustomer.getEmail());
            
            // Lấy order items thật
            List<OrderItem> orderItems = orderService.getOrderItemsByOrder(order);
            model.addAttribute("orderItems", orderItems);
            
            return "order-confirmation";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tạo đơn hàng: " + e.getMessage());
            return "redirect:/cart/checkout";
        }
    }

    // =======================================================
    // 3. TRANG CHỌN ĐỊA CHỈ GIAO HÀNG
    // =======================================================
    @GetMapping("/checkout/select-delivery-address")
    @Transactional(readOnly = true)
    public String selectDeliveryAddressPage(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            Customer currentCustomer = getCurrentCustomer(session);
            if (currentCustomer == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn cần đăng nhập để tiếp tục!");
                return "redirect:/login";
            }
            
            // Lấy địa chỉ hiện tại từ profile
            String currentAddress = currentCustomer.getAddress();
            model.addAttribute("currentAddress", currentAddress);
            
            return "select-delivery-address";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/cart/checkout";
        }
    }

    // =======================================================
    // 4. XỬ LÝ CẬP NHẬT ĐỊA CHỈ GIAO HÀNG
    // =======================================================
    @PostMapping("/checkout/update-delivery-address")
    public String updateDeliveryAddress(
            @RequestParam(value = "addressOption", defaultValue = "profile") String addressOption,
            @RequestParam(value = "finalAddress", required = false) String finalAddress,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            Customer currentCustomer = getCurrentCustomer(session);
            if (currentCustomer == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn cần đăng nhập để tiếp tục!");
                return "redirect:/login";
            }
            
            // Lưu địa chỉ vào session để sử dụng khi đặt hàng
            if ("profile".equals(addressOption)) {
                // Sử dụng địa chỉ từ profile
                session.setAttribute("deliveryAddress", currentCustomer.getAddress());
            } else if ("new".equals(addressOption) && finalAddress != null && !finalAddress.trim().isEmpty()) {
                // Sử dụng địa chỉ mới cho đơn hàng này
                session.setAttribute("deliveryAddress", finalAddress);
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn địa chỉ giao hàng!");
                return "redirect:/cart/checkout/select-delivery-address";
            }
            
            return "redirect:/cart/checkout";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/cart/checkout/select-delivery-address";
        }
    }

    // Trang xác nhận đơn hàng
    @GetMapping("/order-confirmation")
    public String orderConfirmationPage(
            @RequestParam(value = "orderId", required = false) Long orderId,
            Model model, 
            HttpSession session, 
            RedirectAttributes redirectAttributes) {
        try {
            Customer currentCustomer = getCurrentCustomer(session);
            
            // Lấy thông tin khách hàng
            model.addAttribute("customerName", currentCustomer.getFullName());
            model.addAttribute("customerPhone", currentCustomer.getPhone());
            model.addAttribute("customerEmail", currentCustomer.getEmail());
            
            // Lấy đơn hàng vừa tạo
            if (orderId != null) {
                Order order = orderService.getOrderById(orderId);
                
                // Kiểm tra quyền sở hữu
                if (!orderService.isOrderOwnedByCustomer(orderId, currentCustomer)) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền xem đơn hàng này!");
                    return "redirect:/login";
                }
                
                // Lấy thông tin đơn hàng thật
                model.addAttribute("orderCode", "#" + order.getOrderId());
                model.addAttribute("orderId", order.getOrderId().toString());
                model.addAttribute("orderDate", order.getOrderDate());
                model.addAttribute("deliveryAddress", order.getCustomer().getAddress());
                model.addAttribute("orderStatus", order.getStatus());
                model.addAttribute("totalAmount", order.getTotalAmount());
                
                // Lấy order items thật
                List<OrderItem> orderItems = orderService.getOrderItemsByOrder(order);
                model.addAttribute("orderItems", orderItems);
                
                return "order-confirmation";
                
            } else {
                // Fallback nếu không có orderId (trường hợp lỗi)
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy thông tin đơn hàng!");
                return "redirect:/cart";
            }
            
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn cần đăng nhập để xem xác nhận đơn hàng.");
            return "redirect:/login";
        }
    }

    // =======================================================
    // 4. TRANG CHI TIẾT ĐƠN HÀNG
    // =======================================================
    @GetMapping("/order-details/{oid}")
    @Transactional(readOnly = true)
    public String orderDetailsPage(
            @PathVariable("oid") String orderId,
            Model model, 
            HttpSession session, 
            RedirectAttributes redirectAttributes) {
        
        // XÓA TẤT CẢ CÁC ATTRIBUTE CÓ THỂ GHI ĐÈ LÊN ORDERID
        session.removeAttribute("orderId");
        session.removeAttribute("productImage");
        session.removeAttribute("productName");
        session.removeAttribute("sizeName");
        session.removeAttribute("colorName");
        session.removeAttribute("deliveryAddress");
        session.removeAttribute("selectedProductId");
        session.removeAttribute("selectedVariantId");
        
        // DEBUG: In ra tất cả session attributes
        java.util.Enumeration<String> attrs = session.getAttributeNames();
        System.out.println("[DEBUG] Session attributes after cleanup:");
        while (attrs.hasMoreElements()) {
            String attrName = attrs.nextElement();
            Object attrValue = session.getAttribute(attrName);
            System.out.println("  " + attrName + " = " + attrValue);
        }
        
        System.out.println("[CART CONTROLLER] orderDetailsPage called with orderId: " + orderId);
        
        try {
            Customer currentCustomer = getCurrentCustomer(session);
            
            // Chuyển orderId từ String sang Long
            Long orderIdLong;
            try {
                orderIdLong = Long.parseLong(orderId);
            } catch (NumberFormatException e) {
                redirectAttributes.addFlashAttribute("errorMessage", "ID đơn hàng không hợp lệ!");
                return "redirect:/cart";
            }
            
            // Lấy đơn hàng từ database
            Order order = orderService.getOrderById(orderIdLong);
            
            // Kiểm tra quyền sở hữu (customer chỉ xem được đơn hàng của mình)
            if (!orderService.isOrderOwnedByCustomer(orderIdLong, currentCustomer)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền xem đơn hàng này!");
                return "redirect:/cart";
            }
            
            // Lấy thông tin khách hàng
            model.addAttribute("customerName", currentCustomer.getFullName());
            model.addAttribute("customerPhone", currentCustomer.getPhone());
            model.addAttribute("customerEmail", currentCustomer.getEmail());
            // Lấy delivery address an toàn
            String deliveryAddress = (order.getCustomer() != null && order.getCustomer().getAddress() != null) 
                ? order.getCustomer().getAddress() 
                : (currentCustomer.getAddress() != null ? currentCustomer.getAddress() : "N/A");
            model.addAttribute("deliveryAddress", deliveryAddress);
            
            // Lấy thông tin đơn hàng thật
            model.addAttribute("orderCode", "#" + order.getOrderId());
            model.addAttribute("orderId", order.getOrderId().toString());
            model.addAttribute("orderDate", order.getOrderDate());
            model.addAttribute("orderStatus", order.getStatus());
            
            // Lấy order items thật
            List<OrderItem> orderItems = orderService.getOrderItemsByOrder(order);
            model.addAttribute("orderItems", orderItems);
            model.addAttribute("totalAmount", order.getTotalAmount());
            
            System.out.println("[CART CONTROLLER] Order details data:");
            System.out.println("  Order ID: " + order.getOrderId());
            System.out.println("  Customer: " + currentCustomer.getFullName());
            System.out.println("  OrderItems count: " + orderItems.size());
            System.out.println("  Total Amount: " + order.getTotalAmount());
            for (OrderItem item : orderItems) {
                System.out.println("    - Item: " + item.getProductName() + 
                    " | Quantity: " + item.getQuantity() + 
                    " | Total: " + item.getTotalPrice() +
                    " | Size: " + item.getSizeName() +
                    " | Color: " + item.getColorName() +
                    " | Image: " + item.getProductImage());
            }
            
            // Check if user is admin/staff
            String userRole = (String) session.getAttribute("userRole");
            boolean isAdmin = "Admin".equals(userRole);
            boolean isStaff = "Staff".equals(userRole);
            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("isStaff", isStaff);
            
            return "order-details";
            
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn cần đăng nhập để xem chi tiết đơn hàng.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đơn hàng.");
            return "redirect:/cart";
        }
    }

    // =======================================================
    // 5. CẬP NHẬT TRẠNG THÁI ĐƠN HÀNG (CHO ADMIN/STAFF)
    // =======================================================
    @PostMapping("/order-details/{orderId}/update-status")
    public String updateOrderStatus(
            @PathVariable String orderId,
            @RequestParam String newStatus,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            String userRole = (String) session.getAttribute("userRole");
            boolean isAdmin = "Admin".equals(userRole);
            boolean isStaff = "Staff".equals(userRole);
            
            if (!isAdmin && !isStaff) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền cập nhật trạng thái đơn hàng.");
                return "redirect:/order-details/" + orderId;
            }
            
            // Chuyển orderId từ String sang Long
            Long orderIdLong;
            try {
                orderIdLong = Long.parseLong(orderId);
            } catch (NumberFormatException e) {
                redirectAttributes.addFlashAttribute("errorMessage", "ID đơn hàng không hợp lệ!");
                return "redirect:/order-details/" + orderId;
            }
            
            // Chuyển status string sang enum
            vn.edu.fpt.fashionstore.entity.OrderStatus statusEnum;
            try {
                statusEnum = vn.edu.fpt.fashionstore.entity.OrderStatus.valueOf(newStatus);
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Trạng thái không hợp lệ!");
                return "redirect:/order-details/" + orderId;
            }
            
            // Cập nhật trạng thái qua OrderService
            if (statusEnum == vn.edu.fpt.fashionstore.entity.OrderStatus.CONFIRMED) {
                orderService.confirmOrder(orderIdLong, userRole);
            } else if (statusEnum == vn.edu.fpt.fashionstore.entity.OrderStatus.CANCELLED) {
                orderService.cancelOrder(orderIdLong, userRole, "Đã hủy bởi " + userRole);
            }
            
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn hàng thành công!");
            return "redirect:/order-details/" + orderId;
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi cập nhật trạng thái.");
            return "redirect:/order-details/" + orderId;
        }
    }

    // =======================================================
    // 6. HỦY ĐƠN HÀNG (CHO KHÁCH HÀNG)
    // =======================================================
    @PostMapping("/order-details/{oid}/cancel")
    public String cancelOrder(
            @PathVariable("oid") String orderId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            Customer currentCustomer = getCurrentCustomer(session);
            
            // Chuyển orderId từ String sang Long
            Long orderIdLong;
            try {
                orderIdLong = Long.parseLong(orderId);
            } catch (NumberFormatException e) {
                redirectAttributes.addFlashAttribute("errorMessage", "ID đơn hàng không hợp lệ!");
                return "redirect:/order-details/" + orderId;
            }
            
            // Kiểm tra quyền sở hữu và khả năng hủy đơn hàng
            if (!orderService.canCustomerCancelOrder(orderIdLong, currentCustomer)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không thể hủy đơn hàng này!");
                return "redirect:/order-details/" + orderId;
            }
            
            // Hủy đơn hàng qua OrderService
            orderService.cancelOrder(orderIdLong, currentCustomer.getEmail(), "Khách hàng yêu cầu hủy đơn hàng");
            
            redirectAttributes.addFlashAttribute("successMessage", "Đơn hàng đã được hủy thành công.");
            return "redirect:/order-details/" + orderId;
            
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn cần đăng nhập để hủy đơn hàng.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi hủy đơn hàng.");
            return "redirect:/order-details/" + orderId;
        }
    }
}
