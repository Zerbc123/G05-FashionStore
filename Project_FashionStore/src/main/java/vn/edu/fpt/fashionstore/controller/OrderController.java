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
import vn.edu.fpt.fashionstore.entity.Voucher;
import vn.edu.fpt.fashionstore.repository.AccountRepository;
import vn.edu.fpt.fashionstore.repository.OrderRepository;
import vn.edu.fpt.fashionstore.repository.VoucherRepository;
import vn.edu.fpt.fashionstore.service.CartService;
import vn.edu.fpt.fashionstore.service.OrderService;

import jakarta.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;
    
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private VoucherRepository voucherRepository;

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

            // Lấy danh sách voucher hợp lệ từ database
            List<Voucher> validVouchers = voucherRepository.findByIsActiveTrueAndExpiredDateGreaterThanEqual(new java.util.Date());
            model.addAttribute("validVouchers", validVouchers);

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
            @RequestParam(value = "deliveryMethod", required = false) String deliveryMethod,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestParam(value = "totalAmount", defaultValue = "0") Double totalAmount,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // 1. Lấy customer hiện tại
        Customer currentCustomer = getCurrentCustomer(session);
        if (currentCustomer == null) {
            return "redirect:/login";
        }

        // 2. Kiểm tra lỗi nhập liệu (Validation)
        boolean hasError = false;

        if (fullName.trim().isEmpty()) {
            model.addAttribute("errorFullName", "Vui lòng nhập họ và tên của bạn.");
            hasError = true;
        } else if (!fullName.matches("^[\\p{L}\\s]+$")) {
            model.addAttribute("errorFullName", "Họ và tên chỉ được chứa chữ cái.");
            hasError = true;
        }

        if (phone.trim().isEmpty() || !phone.matches("^0[0-9]{9}$")) {
            model.addAttribute("errorPhone", "Số điện thoại không hợp lệ (10 số, bắt đầu bằng 0).");
            hasError = true;
        }

        if (deliveryAddress.trim().isEmpty()) {
            model.addAttribute("errorAddress", "Vui lòng chọn địa chỉ giao hàng.");
            hasError = true;
        }

        // Nếu có lỗi -> Load lại trang checkout
        if (hasError) {
            try {
                List<CartItem> cartItems = cartService.getCartItems(currentCustomer);
                model.addAttribute("cartItems", cartItems);
                model.addAttribute("total", cartService.getCartTotal(cartItems));
                model.addAttribute("validVouchers", voucherRepository.findByIsActiveTrueAndExpiredDateGreaterThanEqual(new java.util.Date()));

                model.addAttribute("fullName", fullName);
                model.addAttribute("phone", phone);
                model.addAttribute("address", deliveryAddress);
                model.addAttribute("note", note);
                return "checkout";
            } catch (Exception e) {
                return "redirect:/login";
            }
        }

        // 3. TẠO ĐƠN HÀNG TRONG DATABASE
        try {
            // Tạo object Order từ giỏ hàng (Service này nên xử lý việc copy items từ Cart sang OrderItem)
            Order order = orderService.createOrderFromCart(currentCustomer, deliveryAddress);

            // Cập nhật các thông tin từ form
            order.setTotalAmount(totalAmount);
            order.setPaymentMethod(paymentMethod); // Quan trọng để biết khách chọn gì

            // Thiết lập trạng thái ban đầu
            if ("MOMO".equals(paymentMethod)) {
                order.setStatus("WAITING_FOR_PAYMENT"); // Chờ thanh toán online
            } else {
                order.setStatus("PENDING"); // Chờ xác nhận (đối với COD)
            }

            // Lưu chính thức vào DB để lấy OrderID
            orderRepository.save(order);

            // Làm sạch Session
            session.removeAttribute("deliveryAddress");
            session.removeAttribute("orderId");
            // ... xóa các session rác khác nếu cần ...

            // 4. KIỂM TRA PHƯƠNG THỨC THANH TOÁN ĐỂ ĐIỀU HƯỚNG
            if ("MOMO".equals(paymentMethod)) {
                // REDIRECT SANG CỔNG THANH TOÁN MOMO
                // Ép kiểu amount sang long vì MoMo không nhận số thập phân
                long amountLong = totalAmount.longValue();
                String orderInfo = "Thanh toan don hang #" + order.getOrderId();

                return "redirect:/fashionstore/payment/momo/create?orderId=" + order.getOrderId()
                        + "&amount=" + amountLong
                        + "&orderInfo=" + orderInfo;
            } else {
                // THANH TOÁN COD: Hiển thị trang hoàn tất đơn hàng trực tiếp
                model.addAttribute("orderCode", "#" + order.getOrderId());
                model.addAttribute("orderId", order.getOrderId().toString());
                model.addAttribute("orderDate", order.getOrderDate());
                model.addAttribute("deliveryAddress", deliveryAddress);
                model.addAttribute("orderStatus", order.getStatus());
                model.addAttribute("totalAmount", order.getTotalAmount());
                model.addAttribute("customerName", currentCustomer.getFullName());
                model.addAttribute("customerPhone", currentCustomer.getPhone());
                model.addAttribute("customerEmail", currentCustomer.getEmail());

                List<OrderItem> orderItems = orderService.getOrderItemsByOrder(order);
                model.addAttribute("orderItems", orderItems);

                return "order-confirmation";
            }

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
            return "redirect:/order/checkout";
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
            return "redirect:/order/checkout";
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
                return "redirect:/order/checkout/select-delivery-address";
            }
            
            return "redirect:/order/checkout";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/order/checkout/select-delivery-address";
        }
    }

    // Trang xác nhận đơn hàng
    @GetMapping("/confirmation")
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
    @GetMapping("/details/{oid}")
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
    @PostMapping("/details/{orderId}/update-status")
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
                return "redirect:/order/details/" + orderId;
            }
            
            // Chuyển orderId từ String sang Long
            Long orderIdLong;
            try {
                orderIdLong = Long.parseLong(orderId);
            } catch (NumberFormatException e) {
                redirectAttributes.addFlashAttribute("errorMessage", "ID đơn hàng không hợp lệ!");
                return "redirect:/order/details/" + orderId;
            }
            
            // Chuyển status string sang enum
            vn.edu.fpt.fashionstore.entity.OrderStatus statusEnum;
            try {
                statusEnum = vn.edu.fpt.fashionstore.entity.OrderStatus.valueOf(newStatus);
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Trạng thái không hợp lệ!");
                return "redirect:/order/details/" + orderId;
            }
            
            // Cập nhật trạng thái qua OrderService
            if (statusEnum == vn.edu.fpt.fashionstore.entity.OrderStatus.CONFIRMED) {
                orderService.confirmOrder(orderIdLong, userRole);
            } else if (statusEnum == vn.edu.fpt.fashionstore.entity.OrderStatus.CANCELLED) {
                orderService.cancelOrder(orderIdLong, userRole, "Đã hủy bởi " + userRole);
            }
            
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn hàng thành công!");
            return "redirect:/order/details/" + orderId;
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi cập nhật trạng thái.");
            return "redirect:/order/details/" + orderId;
        }
    }

    // =======================================================
    // 6. HỦY ĐƠN HÀNG (CHO KHÁCH HÀNG)
    // =======================================================
    @PostMapping("/details/{oid}/cancel")
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
                return "redirect:/order/details/" + orderId;
            }
            
            // Kiểm tra quyền sở hữu và khả năng hủy đơn hàng
            if (!orderService.canCustomerCancelOrder(orderIdLong, currentCustomer)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không thể hủy đơn hàng này!");
                return "redirect:/order/details/" + orderId;
            }
            
            // Hủy đơn hàng qua OrderService
            orderService.cancelOrder(orderIdLong, currentCustomer.getEmail(), "Khách hàng yêu cầu hủy đơn hàng");
            
            redirectAttributes.addFlashAttribute("successMessage", "Đơn hàng đã được hủy thành công.");
            return "redirect:/order/details/" + orderId;
            
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn cần đăng nhập để hủy đơn hàng.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi hủy đơn hàng.");
            return "redirect:/order/details/" + orderId;
        }
    }


    // Thêm phương thức POST này để nhận dữ liệu từ nút "MUA NGAY"
    @PostMapping("/checkout")
    public String buyNowCheckout(
            @RequestParam("productId") Long productId,
            @RequestParam("sizeId") Integer sizeId,
            @RequestParam("colorId") Integer colorId,
            @RequestParam("quantity") Integer quantity,
            Model model, HttpSession session, RedirectAttributes redirectAttributes) {

        try {
            Customer currentCustomer = getCurrentCustomer(session);

            // 1. Logic xử lý "Mua ngay":
            // Thay vì lấy từ Cart, bạn cần lấy thông tin Variant từ productId, sizeId, colorId
            // Sau đó đưa vào model để hiển thị ở trang checkout.

            // TẠM THỜI: Để trang checkout không bị lỗi do thiếu data giỏ hàng:
            // Bạn có thể xử lý logic "Mua ngay" tại đây hoặc chuyển hướng:

            // Lưu thông tin mua ngay vào session nếu trang checkout của bạn
            // đang được viết chỉ để đọc từ giỏ hàng (CartService)
            session.setAttribute("isBuyNow", true);
            session.setAttribute("buyNowProductId", productId);
            session.setAttribute("buyNowSizeId", sizeId);
            session.setAttribute("buyNowColorId", colorId);
            session.setAttribute("buyNowQty", quantity);

            // Sau khi xử lý xong, gọi lại logic hiển thị trang checkout
            return checkoutPage(model, session, redirectAttributes);

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
    }
}
