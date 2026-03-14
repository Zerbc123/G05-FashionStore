package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;

import vn.edu.fpt.fashionstore.entity.Order;
import vn.edu.fpt.fashionstore.entity.OrderItem;
import vn.edu.fpt.fashionstore.entity.OrderStatus;
import vn.edu.fpt.fashionstore.repository.OrderRepository;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderRepository orderRepository;

    private boolean isAdmin(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        return "Admin".equals(role);
    }

    @GetMapping
    public String viewOrders(HttpSession session, Model model) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        List<Order> orders = orderRepository.findAll();

        // Ép kiểu Enum sang String để so sánh cho an toàn
        long pending = orders.stream()
                .filter(o -> "PENDING".equalsIgnoreCase(o.getStatus().name()))
                .count();

        long shipping = orders.stream()
                .filter(o -> "SHIPPING".equalsIgnoreCase(o.getStatus().name()))
                .count();

        long completed = orders.stream()
                .filter(o -> "COMPLETED".equalsIgnoreCase(o.getStatus().name()))
                .count();

        double revenue = orders.stream()
                .filter(o -> "COMPLETED".equalsIgnoreCase(o.getStatus().name()))
                .mapToDouble(Order::getTotalAmount)
                .sum();

        model.addAttribute("orders", orders);
        model.addAttribute("pendingCount", pending);
        model.addAttribute("shippingCount", shipping);
        model.addAttribute("completedCount", completed);
        model.addAttribute("totalRevenue", revenue);

        return "admin/adminorder";
    }

    @PostMapping("/update-status/{id}")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status, HttpSession session, Model model) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        orderRepository.findById(id).ifPresent(order -> {
            String currentStatus = order.getStatus().name();

            // Prevent changing from Completed to Shipping or Pending
            if ("COMPLETED".equalsIgnoreCase(currentStatus) &&
                    ("Shipping".equalsIgnoreCase(status) || "Pending".equalsIgnoreCase(status))) {
                return;
            }

            // Chuyển String nhận từ HTML form sang Enum
            try {
                OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase());
                order.setStatus(newStatus);
                orderRepository.save(order);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid status: " + status);
            }
        });

        return "redirect:/admin/orders";
    }

    @GetMapping("/orderdetails/{id}")
    public String viewOrderDetails(@PathVariable Long id, Model model) {

        Order order = orderRepository.findOrderWithItems(id);
        model.addAttribute("order", order);

        return "admin/vieworderdetail";
    }

    @GetMapping("/orderdetails/{id}/pdf")
    public void exportOrderToPDF(@PathVariable Long id,
                                 HttpServletResponse response) throws IOException {

        Order order = orderRepository.findOrderWithItems(id);

        if (order == null) {
            response.getWriter().write("Không tìm thấy đơn hàng");
            return;
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=don_hang_" + id + ".pdf");

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

        // ===== TIÊU ĐỀ =====
        document.add(new Paragraph("HÓA ĐƠN BÁN HÀNG")
                .setBold()
                .setFontSize(20));

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
    }
}