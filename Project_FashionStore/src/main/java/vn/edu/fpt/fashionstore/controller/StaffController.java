package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff")
public class StaffController {

    // Kiểm tra quyền truy cập STAFF
    private boolean isStaff(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        return "STAFF".equals(role) || "ADMIN".equals(role);
    }

    // Trang Staff View chính
    @GetMapping({"", "/", "/view"})
    public String staffView(HttpSession session, Model model) {
        if (!isStaff(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Staff Panel");
        return "staff/staffview";
    }

    // Staff Dashboard
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!isStaff(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Staff Dashboard");
        return "staff/staffdashboard";
    }

    // Quản lý đơn hàng (Confirm Orders)
    @GetMapping("/orders")
    public String orders(HttpSession session, Model model) {
        if (!isStaff(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Order Management");
        return "staff/conformorder";
    }

    // Customer Support
    @GetMapping("/support")
    public String support(HttpSession session, Model model) {
        if (!isStaff(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Customer Support");
        return "staff/staffsupport";
    }

    // Quản lý kho (Inventory Management)
    @GetMapping("/inventory")
    public String inventory(HttpSession session, Model model) {
        if (!isStaff(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Inventory Management");
        return "staff/inventory";
    }
}