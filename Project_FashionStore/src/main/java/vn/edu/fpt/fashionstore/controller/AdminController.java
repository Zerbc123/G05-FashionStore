package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.service.AccountService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AccountService accountService;

    // Kiểm tra quyền truy cập ADMIN
    private boolean isAdmin(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        return "Admin".equals(role);
    }

    // Trang Admin View chính
    @GetMapping("")
    public String adminView(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Admin Panel");
        return "admin/view_admin";
    }

    // Admin Dashboard
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Admin Dashboard");
        return "admin/admindashboard";
    }

    // Quản lý sản phẩm
    @GetMapping("/products")
    public String products(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Product Management");
        return "admin/adminproduct";
    }

    // Thêm sản phẩm mới
    @GetMapping("/products/add")
    public String addProduct(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Add New Product");
        return "admin/addproduct";
    }

    // Chỉnh sửa sản phẩm
    @GetMapping("/edit")
    public String editProduct(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Edit Product");
        return "admin/edit";
    }

    // Quản lý đơn hàng
    @GetMapping("/orders")
    public String orders(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Order Management");
        return "admin/adminorder";
    }

    // Xem chi tiết đơn hàng
    @GetMapping("/orderdetails/{id}")
    public String orderDetails(@PathVariable String id, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Order Details");
        model.addAttribute("orderId", id);
        return "orderdetails";
    }

    // Quản lý khách hàng
    @GetMapping("/customers")
    public String customers(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Customer Management");
        return "admin/admincustomer";
    }

    // Xem chi tiết khách hàng
    @GetMapping("/customers/details/{id}")
    public String customerDetails(@PathVariable String id, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Customer Details");
        model.addAttribute("customerId", id);
        return "profile";
    }

    // Chỉnh sửa khách hàng
    @GetMapping("/customers/edit/{id}")
    public String editCustomer(@PathVariable String id, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Edit Customer");
        model.addAttribute("customerId", id);
        return "editprofile";
    }

    // Quản lý nhân viên
    @GetMapping("/staff")
    public String staff(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Staff Management");
        return "admin/view_staff";
    }

    // Thêm nhân viên mới
    @GetMapping("/staff/add")
    public String addStaff(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Add New Staff");
        return "admin/add_new_staff";
    }

    // Xem chi tiết nhân viên
    @GetMapping("/staffdetails/{id}")
    public String staffDetails(@PathVariable String id, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Staff Details");
        model.addAttribute("staffId", id);
        return "admin/view_staff_details";
    }

    // Hủy/Xóa nhân viên
    @GetMapping("/cancel_staff")
    public String cancelStaff(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Cancel Staff");
        return "admin/cancel_staff";
    }

    // Báo cáo
    @GetMapping("/reports")
    public String reports(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Reports");
        return "admin/reports";
    }

    // ==================== ADMIN CHANGE PASSWORD ====================

    @GetMapping("/change-password")
    public String adminChangePasswordPage(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Change Password");
        return "admin/admin_change_password";
    }

    @PostMapping("/change-password")
    public String adminHandleChangePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session,
            RedirectAttributes ra) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        String email = (String) session.getAttribute("user");
        if (email == null) {
            return "redirect:/login";
        }

        // 1. Kiểm tra mật khẩu mới khớp nhau
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Mật khẩu mới và xác nhận không khớp!");
            return "redirect:/admin/change-password";
        }

        // 2. Giới hạn mật khẩu tối thiểu 6 ký tự
        if (newPassword.length() < 6) {
            ra.addFlashAttribute("error", "Mật khẩu mới phải có ít nhất 6 ký tự!");
            return "redirect:/admin/change-password";
        }

        // 3. Gọi service để đổi mật khẩu
        boolean success = accountService.changePassword(email, currentPassword, newPassword);

        if (success) {
            ra.addFlashAttribute("success", "Đổi mật khẩu thành công!");
            return "redirect:/admin/profile";
        } else {
            ra.addFlashAttribute("error", "Mật khẩu hiện tại không đúng!");
            return "redirect:/admin/change-password";
        }
    }
}
