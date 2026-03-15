package vn.edu.fpt.fashionstore.controller;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.entity.CartItem;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.entity.Order;
import vn.edu.fpt.fashionstore.entity.OrderItem;
import vn.edu.fpt.fashionstore.entity.Voucher;
import vn.edu.fpt.fashionstore.service.CartService;
import vn.edu.fpt.fashionstore.service.OrderService;

import jakarta.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/order")
public class OrderController {

    private final CartService cartService;
    private final OrderService orderService;

    public OrderController(
            CartService cartService,
            OrderService orderService) {
        this.cartService = cartService;
        this.orderService = orderService;
    }

    // Lấy customer từ session - chuyển sang service
    private Customer getCurrentCustomer(HttpSession session) {
        return orderService.getCurrentCustomer(session);
    }

    // =======================================================
    // 1. MỞ TRANG CHECKOUT
    // =======================================================
    @GetMapping("/checkout")
    @Transactional(readOnly = true)
    public String checkoutPage(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            System.out.println("=== CHECKOUT PAGE START ===");
            Customer currentCustomer = getCurrentCustomer(session);
            if (currentCustomer == null) {
                System.out.println("No customer found, redirecting to login");
                return "redirect:/login";
            }
            
            List<CartItem> cartItems = orderService.getCartItemsForCheckout(session, currentCustomer);
            
            System.out.println("Found cart items: " + (cartItems != null ? cartItems.size() : 0));

            // Nếu giỏ hàng trống thì không cho vào trang checkout
            if (cartItems == null || cartItems.isEmpty()) {
                System.out.println("Cart is empty, redirecting to cart");
                redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng của bạn đang trống!");
                return "redirect:/cart";
            }

            double total = orderService.calculateCartTotal(cartItems);
            System.out.println("Cart total: " + total);

            // Gửi dữ liệu ra cột bên phải
            model.addAttribute("cartItems", cartItems);
            model.addAttribute("total", total);

            // Lấy danh sách voucher hợp lệ từ database
            List<Voucher> validVouchers = orderService.getValidVouchers();
            model.addAttribute("validVouchers", validVouchers);

            // Lấy sẵn tên và sđt từ Customer điền sẵn vào form cho khách lười gõ
            model.addAttribute("fullName", currentCustomer.getFullName());
            
            // Sử dụng chuỗi điện thoại trực tiếp (đã lưu với số 0 đầu nếu có)
            String phoneStr = currentCustomer.getPhone();
            if (phoneStr == null) phoneStr = "";
            model.addAttribute("phone", phoneStr);
            
            // Ưu tiên địa chỉ từ session (đã chọn từ trang chọn địa chỉ), nếu không có thì dùng từ profile
            String deliveryAddress = orderService.getDeliveryAddressFromSession(session, currentCustomer);
            model.addAttribute("address", deliveryAddress);

            System.out.println("=== CHECKOUT PAGE SUCCESS ===");
            return "checkout";
        } catch (RuntimeException e) {
            System.err.println("Error in checkoutPage: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/cart";
        } catch (Exception e) {
            System.err.println("Unexpected error in checkoutPage: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống! Vui lòng thử lại.");
            return "redirect:/cart";
        }
    }

    // =======================================================
    // 2. XỬ LÝ KHI BẤM NÚT "ĐẶT HÀNG" (VALIDATION CHUYỂN SANG SERVICE)
    // =======================================================
    @PostMapping("/checkout/place-order")
    public String placeOrder(
            @RequestParam(value = "fullName", defaultValue = "") String fullName,
            @RequestParam(value = "phone", defaultValue = "") String phone,
            @RequestParam(value = "deliveryAddress", defaultValue = "") String deliveryAddress,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "deliveryMethod", required = false) String deliveryMethod,
            @RequestParam(value = "paymentMethod", required = false) String paymentMethod,
            @RequestParam(value = "totalAmount", defaultValue = "0") Double totalAmount,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // Lấy customer hiện tại
        Customer currentCustomer = getCurrentCustomer(session);
        
        // Kiểm tra nếu customer không tồn tại
        if (currentCustomer == null) {
            return "redirect:/login";
        }

        // Validation qua service
        try {
            orderService.validateCheckoutData(fullName, phone, deliveryAddress);
        } catch (RuntimeException e) {
            // Nếu có lỗi -> Load lại danh sách sản phẩm và trả về trang checkout kèm lỗi
            try {
                List<CartItem> cartItems = orderService.getCartItemsForValidation(session, currentCustomer);
                double subtotal = orderService.calculateCartTotal(cartItems);

                // Gửi lại data giỏ hàng
                model.addAttribute("cartItems", cartItems);
                model.addAttribute("total", subtotal);
                
                // Lấy danh sách voucher hợp lệ từ database
                List<Voucher> validVouchers = orderService.getValidVouchers();
                model.addAttribute("validVouchers", validVouchers);

                // Giữ nguyên chữ khách đã nhập
                model.addAttribute("fullName", fullName);
                model.addAttribute("phone", phone);
                model.addAttribute("address", deliveryAddress);
                model.addAttribute("note", note);
                model.addAttribute("errorFullName", e.getMessage().contains("tên") ? e.getMessage() : null);
                model.addAttribute("errorPhone", e.getMessage().contains("điện thoại") ? e.getMessage() : null);
                model.addAttribute("errorAddress", e.getMessage().contains("địa chỉ") ? e.getMessage() : null);

                return "checkout"; // Trả lại trang để khách sửa lỗi
            } catch (Exception ex) {
                return "redirect:/login";
            }
        }
        
        // TẠO ĐƠN HÀNG THỰC TẾ QUA ORDER SERVICE
        try {
            Order order = orderService.createOrderFromSessionDataWithTotal(session, currentCustomer, deliveryAddress, totalAmount);
            
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
    // 4. XỬ LÝ CẬP NHẬT ĐỊA CHỈ GIAO HÀNG (CHUYỂN LOGIC SANG SERVICE)
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
            
            // Gọi service để xử lý logic
            orderService.updateDeliveryAddressInSession(session, currentCustomer, addressOption, finalAddress);
            
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
}
