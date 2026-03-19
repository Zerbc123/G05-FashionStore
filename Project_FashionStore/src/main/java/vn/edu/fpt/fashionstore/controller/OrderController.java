package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;

import vn.edu.fpt.fashionstore.entity.*;
import vn.edu.fpt.fashionstore.repository.*;
import vn.edu.fpt.fashionstore.service.*;

import java.io.IOException;
import java.util.*;

@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final ReturnRequestService returnRequestService;
    private final CartService cartService;
    private final OrderService orderService;
    private final AccountRepository accountRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherService voucherService;
    private final MomoService momoService;

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

            if (cartItems == null || cartItems.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng của bạn đang trống!");
                return "redirect:/cart";
            }

            double total = cartService.getCartTotal(cartItems);
            double discountAmount = 0.0;
            double finalTotal = total;

            // KIỂM TRA XEM CÓ VOUCHER TRONG SESSION KHÔNG
            Voucher appliedVoucher = (Voucher) session.getAttribute("appliedVoucher");

            if (appliedVoucher != null) {
                if (appliedVoucher.getMinOrderValue() != null && total < appliedVoucher.getMinOrderValue()) {
                    session.removeAttribute("appliedVoucher");
                    model.addAttribute("errorMessage", "Mã giảm giá đã bị gỡ vì giỏ hàng không đủ điều kiện (" + String.format("%,.0f", appliedVoucher.getMinOrderValue()) + "đ)!");
                } else {
                    discountAmount = appliedVoucher.getDiscountValue();
                    finalTotal = total - discountAmount;
                    if (finalTotal < 0) finalTotal = 0;
                    model.addAttribute("appliedVoucher", appliedVoucher);
                }
            }

            model.addAttribute("cartItems", cartItems);
            model.addAttribute("total", total);
            model.addAttribute("discountAmount", discountAmount);
            model.addAttribute("finalTotal", finalTotal);

            // Lấy danh sách voucher hợp lệ từ database
            List<Voucher> validVouchers = voucherRepository.findByIsActiveTrueAndExpiredDateGreaterThanEqual(LocalDate.now());
            model.addAttribute("validVouchers", validVouchers);

            model.addAttribute("fullName", currentCustomer.getFullName());
            String phoneStr = currentCustomer.getPhone();
            if (phoneStr == null) phoneStr = "";
            model.addAttribute("phone", phoneStr);

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
    // HÀM MỚI: XỬ LÝ ÁP DỤNG VOUCHER TỪ TRANG CHECKOUT
    // =======================================================
    @PostMapping("/apply-voucher")
    public String applyVoucher(
            @RequestParam("voucherCode") String voucherCode,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            Customer currentCustomer = getCurrentCustomer(session);
            List<CartItem> cartItems = cartService.getCartItems(currentCustomer);
            double cartTotal = cartService.getCartTotal(cartItems);

            if (voucherCode == null || voucherCode.trim().isEmpty()) {
                session.removeAttribute("appliedVoucher");
                redirectAttributes.addFlashAttribute("successMessage", "Đã hủy áp dụng mã giảm giá.");
                return "redirect:/order/checkout";
            }

            Voucher voucher = voucherService.getValidVoucherByCode(voucherCode);

            if (voucher != null) {
                if (voucher.getMinOrderValue() != null && cartTotal < voucher.getMinOrderValue()) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "Đơn hàng phải từ " + String.format("%,.0f", voucher.getMinOrderValue()) + "đ mới được áp dụng mã này!");
                } else {
                    session.setAttribute("appliedVoucher", voucher);
                    redirectAttributes.addFlashAttribute("successMessage", "Áp dụng voucher thành công!");
                }
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Mã giảm giá không hợp lệ hoặc đã hết hạn!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/order/checkout";
    }

    // =======================================================
    // 2. XỬ LÝ KHI BẤM NÚT "ĐẶT HÀNG"
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

        Customer currentCustomer = getCurrentCustomer(session);
        if (currentCustomer == null) {
            return "redirect:/login";
        }

        boolean hasError = false;

        if (fullName.trim().isEmpty()) {
            model.addAttribute("errorFullName", "Vui lòng nhập họ và tên của bạn.");
            hasError = true;
        } else if (!fullName.matches("^[\\p{L}\\s]+$")) {
            model.addAttribute("errorFullName", "Họ và tên chỉ được chứa chữ cái, không nhập số hay ký tự đặc biệt.");
            hasError = true;
        }

        if (phone.trim().isEmpty() || !phone.matches("^0[0-9]{9}$")) {
            model.addAttribute("errorPhone", "Số điện thoại không hợp lệ (Bắt buộc 10 số và bắt đầu bằng 0).");
            hasError = true;
        }

        if (deliveryAddress.trim().isEmpty()) {
            model.addAttribute("errorAddress", "Vui lòng chọn địa chỉ giao hàng.");
            hasError = true;
        }

        if (hasError) {
            return checkoutPage(model, session, redirectAttributes);
        }

        try {
            if ("MOMO".equals(paymentMethod)) {
                session.setAttribute("momo_deliveryAddress", deliveryAddress);
                session.setAttribute("momo_totalAmount", totalAmount);

                String orderIdStr = "ORD-" + System.currentTimeMillis();
                String orderInfo = "Thanh toan don hang " + fullName;
                String requestId = vn.edu.fpt.fashionstore.util.MomoUtils.generateRequestId();

                Map<String, Object> response = momoService.createPaymentRequest(orderIdStr, totalAmount, orderInfo, requestId);

                if (response != null && response.containsKey("payUrl")) {
                    String payUrl = (String) response.get("payUrl");
                    return "redirect:" + payUrl;
                } else {
                    redirectAttributes.addFlashAttribute("errorMessage", "Không thể tạo yêu cầu thanh toán MOMO");
                    return "redirect:/order/checkout";
                }
            }

            Order order = orderService.createOrderFromCart(currentCustomer, deliveryAddress);
            order.setTotalAmount(totalAmount);
            orderRepository.save(order);

            session.removeAttribute("deliveryAddress");
            session.removeAttribute("orderId");
            session.removeAttribute("appliedVoucher");

            redirectAttributes.addFlashAttribute("successMessage", "Đặt hàng thành công! Mã đơn hàng của bạn: #" + order.getOrderId());

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
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tạo đơn hàng: " + e.getMessage());
            return "redirect:/order/checkout";
        }
    }

    // =======================================================
    // XỬ LÝ KHI MOMO TRẢ VỀ THÀNH CÔNG
    // =======================================================
    @GetMapping("/checkout/momo-success")
    public String momoSuccess(@RequestParam(value = "momoOrderId", required = false) String momoOrderId,
                              @RequestParam(value = "transId", required = false) String transId,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Customer currentCustomer;
        try {
            currentCustomer = getCurrentCustomer(session);
        } catch (Exception e) {
            return "redirect:/login";
        }

        String deliveryAddress = (String) session.getAttribute("momo_deliveryAddress");
        Double totalAmount = (Double) session.getAttribute("momo_totalAmount");

        if (deliveryAddress == null || totalAmount == null) {
            return "redirect:/order/confirmation";
        }

        try {
            Order order = orderService.createOrderFromCart(currentCustomer, deliveryAddress);
            order.setTotalAmount(totalAmount);
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            session.removeAttribute("momo_deliveryAddress");
            session.removeAttribute("momo_totalAmount");
            session.removeAttribute("deliveryAddress");
            session.removeAttribute("appliedVoucher");

            redirectAttributes.addFlashAttribute("successMessage", "Thanh toán MOMO thành công! Mã đơn hàng: #" + order.getOrderId());
            
            return "redirect:/order/confirmation?orderId=" + order.getOrderId();

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

            if ("profile".equals(addressOption)) {
                session.setAttribute("deliveryAddress", currentCustomer.getAddress());
            } else if ("new".equals(addressOption) && finalAddress != null && !finalAddress.trim().isEmpty()) {
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

    // =======================================================
    // 5. TRANG XÁC NHẬN ĐƠN HÀNG
    // =======================================================
    @GetMapping("/confirmation")
    public String orderConfirmationPage(
            @RequestParam(value = "orderId", required = false) Long orderId,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            Customer currentCustomer = getCurrentCustomer(session);

            model.addAttribute("customerName", currentCustomer.getFullName());
            model.addAttribute("customerPhone", currentCustomer.getPhone());
            model.addAttribute("customerEmail", currentCustomer.getEmail());

            if (orderId != null) {
                Order order = orderService.getOrderById(orderId);

                if (!orderService.isOrderOwnedByCustomer(orderId, currentCustomer)) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền xem đơn hàng này!");
                    return "redirect:/login";
                }

                model.addAttribute("orderCode", "#" + order.getOrderId());
                model.addAttribute("orderId", order.getOrderId().toString());
                model.addAttribute("orderDate", order.getOrderDate());
                model.addAttribute("deliveryAddress", order.getCustomer().getAddress());
                model.addAttribute("orderStatus", order.getStatus());
                model.addAttribute("totalAmount", order.getTotalAmount());

                List<OrderItem> orderItems = orderService.getOrderItemsByOrder(order);
                model.addAttribute("orderItems", orderItems);

                return "order-confirmation";

            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy thông tin đơn hàng!");
                return "redirect:/cart";
            }

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn cần đăng nhập để xem xác nhận đơn hàng.");
            return "redirect:/login";
        }
    }

    // =======================================================
    // 6. TRANG CHI TIẾT ĐƠN HÀNG
    // =======================================================
    @GetMapping("/details/{oid}")
    @Transactional(readOnly = true)
    public String orderDetailsPage(
            @PathVariable("oid") String orderId,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

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

            Long orderIdLong;
            try {
                orderIdLong = Long.parseLong(orderId);
            } catch (NumberFormatException e) {
                redirectAttributes.addFlashAttribute("errorMessage", "ID đơn hàng không hợp lệ!");
                return "redirect:/cart";
            }

            Order order = orderService.getOrderById(orderIdLong);

            if (!orderService.isOrderOwnedByCustomer(orderIdLong, currentCustomer)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền xem đơn hàng này!");
                return "redirect:/cart";
            }

            model.addAttribute("customerName", currentCustomer.getFullName());
            model.addAttribute("customerPhone", currentCustomer.getPhone());
            model.addAttribute("customerEmail", currentCustomer.getEmail());
            String deliveryAddress = (order.getCustomer() != null && order.getCustomer().getAddress() != null)
                    ? order.getCustomer().getAddress()
                    : (currentCustomer.getAddress() != null ? currentCustomer.getAddress() : "N/A");
            model.addAttribute("deliveryAddress", deliveryAddress);

            model.addAttribute("orderCode", "#" + order.getOrderId());
            model.addAttribute("orderId", order.getOrderId().toString());
            model.addAttribute("orderDate", order.getOrderDate());
            model.addAttribute("orderStatus", order.getStatus());

            List<OrderItem> orderItems = orderService.getOrderItemsByOrder(order);
            model.addAttribute("orderItems", orderItems);
            model.addAttribute("totalAmount", order.getTotalAmount());

            String userRole = (String) session.getAttribute("userRole");
            boolean isAdmin = "Admin".equals(userRole);
            boolean isStaff = "Staff".equals(userRole);
            model.addAttribute("isAdmin", isAdmin);
            model.addAttribute("isStaff", isStaff);

            boolean hasReturnRequest = returnRequestService.hasReturnRequest(order);
            model.addAttribute("hasReturnRequest", hasReturnRequest);
            if (hasReturnRequest) {
                model.addAttribute("returnRequest", returnRequestService.getReturnRequestByOrder(order));
            }

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
    // 7. XỬ LÝ KHÁCH HÀNG YÊU CẦU TRẢ HÀNG
    // =======================================================
    @PostMapping("/details/{oid}/return")
    public String requestReturn(
            @PathVariable("oid") String orderId,
            @RequestParam("reason") String reason,
            @RequestParam("description") String description,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            Customer currentCustomer = getCurrentCustomer(session);
            Long orderIdLong = Long.parseLong(orderId);
            Order order = orderService.getOrderById(orderIdLong);

            if (order.getStatus() != OrderStatus.COMPLETED) {
                redirectAttributes.addFlashAttribute("errorMessage", "Chỉ có thể trả hàng khi đơn hàng đã giao thành công!");
                return "redirect:/order/details/" + orderId;
            }

            returnRequestService.createReturnRequest(order, currentCustomer, reason, description);

            redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu Trả hàng/Hoàn tiền đã được gửi. Shop sẽ phản hồi sớm nhất!");
            return "redirect:/order/details/" + orderId;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/order/details/" + orderId;
        }
    }

    // =======================================================
    // 9. HỦY ĐƠN HÀNG (CHO KHÁCH HÀNG)
    // =======================================================
    @PostMapping("/details/{oid}/cancel")
    public String cancelOrder(
            @PathVariable("oid") String orderId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            Customer currentCustomer = getCurrentCustomer(session);

            Long orderIdLong;
            try {
                orderIdLong = Long.parseLong(orderId);
            } catch (NumberFormatException e) {
                redirectAttributes.addFlashAttribute("errorMessage", "ID đơn hàng không hợp lệ!");
                return "redirect:/order/details/" + orderId;
            }

            if (!orderService.canCustomerCancelOrder(orderIdLong, currentCustomer)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không thể hủy đơn hàng này!");
                return "redirect:/order/details/" + orderId;
            }

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

    // =======================================================
    // 10. BUY NOW CHECKOUT
    // =======================================================
    @PostMapping("/checkout")
    public String buyNowCheckout(
            @RequestParam("productId") Long productId,
            @RequestParam("sizeId") Integer sizeId,
            @RequestParam("colorId") Integer colorId,
            @RequestParam("quantity") Integer quantity,
            Model model, HttpSession session, RedirectAttributes redirectAttributes) {

        try {
            Customer currentCustomer = getCurrentCustomer(session);
            session.setAttribute("isBuyNow", true);
            session.setAttribute("buyNowProductId", productId);
            session.setAttribute("buyNowSizeId", sizeId);
            session.setAttribute("buyNowColorId", colorId);
            session.setAttribute("buyNowQty", quantity);

            return checkoutPage(model, session, redirectAttributes);

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
    }

    // =======================================================
    // 11. PDF EXPORT
    // =======================================================
    @GetMapping("/details/{id}/pdf")
    public void exportOrderToPDF(@PathVariable Long id,
                                 HttpServletResponse response) throws IOException {

        Order order = orderService.getOrderById(id);

        if (order == null) {
            response.getWriter().write("Không tìm thấy đơn hàng");
            return;
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=don_hang_" + id + ".pdf");

        PdfWriter writer = new PdfWriter(response.getOutputStream());
        PdfDocument pdf = new PdfDocument(writer);

        PdfFont font = PdfFontFactory.createFont(
                getClass().getResource("/fonts/arial.ttf").toExternalForm(),
                PdfEncodings.IDENTITY_H,
                pdf);

        Document document = new Document(pdf);
        document.setFont(font);
        document.add(new Paragraph("Fashion store")
                .setBold()
                .setFontSize(25));

        document.add(new Paragraph("HÓA ĐƠN BÁN HÀNG")
                .setBold()
                .setFontSize(20));

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Mã đơn hàng: " + order.getOrderId()));
        document.add(new Paragraph("Tên người đặt: "
                + order.getCustomer().getFullName()));
        document.add(new Paragraph("Số điện thoại: "
                + order.getCustomer().getPhone()));
        document.add(new Paragraph("Địa chỉ giao hàng: "
                + order.getShippingAddress()));
        document.add(new Paragraph("Ngày đặt: "
                + order.getOrderDate()));
        document.add(new Paragraph(" "));

        Table table = new Table(4);
        table.addHeaderCell("Sản phẩm");
        table.addHeaderCell("Đơn giá");
        table.addHeaderCell("Số lượng");
        table.addHeaderCell("Thành tiền");

        double tongTien = 0;

        for (OrderItem item : order.getOrderItems()) {
            double gia = item.getTotalPrice() != null ? item.getTotalPrice() : 0;
            int soLuong = item.getQuantity() != null ? item.getQuantity() : 0;
            double thanhTien = gia * soLuong;

            tongTien += thanhTien;

            table.addCell(item.getProductVariant()
                    .getProduct()
                    .getProductName());

            table.addCell(String.format("%,.0f VNĐ", gia));
            table.addCell(String.valueOf(soLuong));
            table.addCell(String.format("%,.0f VNĐ", thanhTien));
        }

        document.add(table);

        document.add(new Paragraph(" "));
        document.add(new Paragraph("TỔNG TIỀN: "
                + String.format("%,.0f VNĐ", tongTien))
                .setBold());

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Cảm ơn quý khách đã mua hàng!"));

        document.close();
    }
}
