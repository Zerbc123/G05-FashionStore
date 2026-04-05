package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;

import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.fashionstore.entity.Order;
import vn.edu.fpt.fashionstore.entity.OrderItem;
import vn.edu.fpt.fashionstore.entity.OrderStatus;
import vn.edu.fpt.fashionstore.repository.OrderRepository;
import vn.edu.fpt.fashionstore.service.OrderService;
import vn.edu.fpt.fashionstore.util.RoleUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    private boolean isAdmin(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        return "Admin".equals(role);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String viewOrders(HttpSession session, Model model) {

        if (!RoleUtils.canViewOrders(session)) {
            return "redirect:/login";
        }

        // Sử dụng query có FETCH để lấy luôn Customer, tránh lỗi Lazy loading trong view
        List<Order> orders = orderRepository.findOrdersForAdmin();

        // Ép kiểu Enum sang String để so sánh cho an toàn
        long pending = orders.stream()
                .filter(o -> "PENDING".equalsIgnoreCase(o.getStatus().name()))
                .count();

        long confirmed = orders.stream()
                .filter(o -> "CONFIRMED".equalsIgnoreCase(o.getStatus().name()))
                .count();

        long cancelled = orders.stream()
                .filter(o -> "CANCELLED".equalsIgnoreCase(o.getStatus().name()))
                .count();

        double revenue = orders.stream()
                .filter(o -> "CONFIRMED".equalsIgnoreCase(o.getStatus().name()))
                .mapToDouble(Order::getTotalAmount)
                .sum();

        model.addAttribute("orders", orders);
        model.addAttribute("pendingCount", pending);
        model.addAttribute("confirmedCount", confirmed);
        model.addAttribute("cancelledCount", cancelled);
        model.addAttribute("totalRevenue", revenue);

        return "admin/adminorder";
    }

    @PostMapping("/update-status/{id}")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status, 
                               HttpSession session, 
                               RedirectAttributes redirectAttributes) {

        if (!RoleUtils.canManageOrders(session)) {
            return "redirect:/login";
        }

        String userRole = (String) session.getAttribute("userRole");
        if (userRole == null) {
            userRole = "Admin";
        }

        try {
            // Chuyển String nhận từ HTML form sang Enum
            OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase().trim());

            // Gọi service để xử lý logic trừ/hoàn stock
            if (newStatus == OrderStatus.CONFIRMED) {
                orderService.confirmOrder(id, userRole);
                redirectAttributes.addFlashAttribute("success", "Đã xác nhận đơn hàng và trừ stock thành công!");
            } else if (newStatus == OrderStatus.CANCELLED) {
                orderService.cancelOrder(id, userRole, "Đã hủy bởi " + userRole);
                redirectAttributes.addFlashAttribute("success", "Đã hủy đơn hàng và hoàn lại stock thành công!");
            } else {
                // Các trạng thái khác - gọi qua service riêng để có transaction sạch
                orderService.updateOrderStatus(id, newStatus);
                redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái đơn hàng thành công!");
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Trạng thái không hợp lệ: " + e.getMessage());
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/orders";
    }

    @GetMapping("/orderdetails/{id}")
    @Transactional(readOnly = true)
    public String viewOrderDetails(@PathVariable Long id, HttpSession session, Model model) {

        if (!RoleUtils.canViewOrders(session)) {
            return "redirect:/login";
        }

        Order order = orderRepository.findOrderWithItems(id);
        model.addAttribute("order", order);

        return "admin/vieworderdetail";
    }

    @GetMapping("/orderdetails/{id}/pdf")
    @Transactional(readOnly = true)
    public void exportOrderToPDF(@PathVariable Long id,
                                 HttpSession session,
                                 HttpServletResponse response) throws IOException {

        if (!RoleUtils.canViewOrders(session)) {
            response.sendRedirect("/login");
            return;
        }

        Order order = orderRepository.findOrderWithItems(id);
        if (order == null) {
            response.getWriter().write("Không tìm thấy đơn hàng");
            return;
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=don_hang_" + id + ".pdf");

        try {
            PdfWriter writer = new PdfWriter(response.getOutputStream());
            PdfDocument pdf = new PdfDocument(writer);

            // ===== LOAD FONT TIẾNG VIỆT =====
            PdfFont font = PdfFontFactory.createFont(
                    getClass().getResource("/fonts/arial.ttf").toExternalForm(),
                    PdfEncodings.IDENTITY_H,
                    pdf);

            Document document = new Document(pdf);
            document.setFont(font);
            document.add(new Paragraph("Fashion store")
                    .setBold()
                    .setFontSize(25));

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Mã đơn hàng: #" + order.getOrderId()));
            document.add(new Paragraph("Tên người đặt: " + order.getCustomer().getFullName()));
            document.add(new Paragraph("Số điện thoại: " + order.getCustomer().getPhone()));
            document.add(new Paragraph("Địa chỉ giao hàng: " + order.getShippingAddress()));
            document.add(new Paragraph("Ngày đặt: " + order.getOrderDate()));
            document.add(new Paragraph(" "));

            // ===== BẢNG SẢN PHẨM =====
            Table table = new Table(4);
            table.addHeaderCell("Sản phẩm");
            table.addHeaderCell("Đơn giá");
            table.addHeaderCell("Số lượng");
            table.addHeaderCell("Thành tiền");

            double tongTien = 0;

            for (OrderItem item : order.getOrderItems()) {

                // Đã sửa lại thành getTotalPrice() cho khớp với Entity OrderItem của bạn
                double thanhTien = item.getTotalPrice() != null ? item.getTotalPrice() : 0;
                int soLuong = item.getQuantity() != null ? item.getQuantity() : 0;

                // Tính ngược lại đơn giá để in ra bảng
                double gia = (soLuong > 0) ? (thanhTien / soLuong) : 0;

                tongTien += thanhTien;

                table.addCell(item.getProductVariant().getProduct().getProductName());
                table.addCell(String.format("%,.0f VNĐ", gia));
                table.addCell(String.valueOf(soLuong));
                table.addCell(String.format("%,.0f VNĐ", thanhTien));
            }

            document.add(table);

            document.add(new Paragraph(" "));
            document.add(new Paragraph("TỔNG TIỀN: " + String.format("%,.0f VNĐ", tongTien)).setBold());

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Cảm ơn quý khách đã mua hàng!"));

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("Lỗi khi tạo PDF: " + e.getMessage());
        }
    }
}