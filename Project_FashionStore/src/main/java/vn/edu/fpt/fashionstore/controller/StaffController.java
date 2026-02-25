package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.repository.AccountRepository;
import vn.edu.fpt.fashionstore.service.AccountService;

@Controller
@RequestMapping("/staff")
public class StaffController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    private boolean isStaff(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        return "Staff".equals(role) || "Admin".equals(role);
    }

    // ======== STAFF VIEW ========

    @GetMapping({"", "/", "/view"})
    public String staffView(HttpSession session, Model model) {
        if (!isStaff(session)) return "redirect:/login";
        model.addAttribute("title", "Staff Panel");
        return "staff/staffview";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!isStaff(session)) return "redirect:/login";
        model.addAttribute("title", "Staff Dashboard");
        return "staff/staffdashboard";
    }

    @GetMapping("/orders")
    public String orders(HttpSession session, Model model) {
        if (!isStaff(session)) return "redirect:/login";
        model.addAttribute("title", "Order Management");
        return "staff/conformorder";
    }

    @GetMapping("/support")
    public String support(HttpSession session, Model model) {
        if (!isStaff(session)) return "redirect:/login";
        model.addAttribute("title", "Customer Support");
        return "staff/staffsupport";
    }

    @GetMapping("/inventory")
    public String inventory(HttpSession session, Model model) {
        if (!isStaff(session)) return "redirect:/login";
        model.addAttribute("title", "Inventory Management");
        return "staff/inventory";
    }

    // ======== CHANGE PASSWORD ========

    @GetMapping("/change-password")
    public String staffChangePasswordPage(HttpSession session, Model model) {
        if (!isStaff(session)) return "redirect:/login";
        model.addAttribute("title", "Change Password");
        return "staff/staff_change_password";
    }

    @PostMapping("/change-password")
    public String staffHandleChangePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session,
            RedirectAttributes ra) {

        if (!isStaff(session)) return "redirect:/login";

        String email = (String) session.getAttribute("user");
        if (email == null) return "redirect:/login";

        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Mật khẩu mới và xác nhận không khớp!");
            return "redirect:/staff/change-password";
        }

        if (newPassword.length() < 6) {
            ra.addFlashAttribute("error", "Mật khẩu mới phải có ít nhất 6 ký tự!");
            return "redirect:/staff/change-password";
        }

        boolean success = accountService.changePassword(email, currentPassword, newPassword);

        if (success) {
            ra.addFlashAttribute("success", "Đổi mật khẩu thành công!");
            return "redirect:/staff/profile";
        } else {
            ra.addFlashAttribute("error", "Mật khẩu hiện tại không đúng!");
            return "redirect:/staff/change-password";
        }
    }

    // ======== STAFF PROFILE ========

    @GetMapping("/profile")
    public String staffProfile(HttpSession session, Model model) {
        if (!isStaff(session)) return "redirect:/login";

        String email = (String) session.getAttribute("user");
        if (email == null) return "redirect:/login";

        var staff = accountService.getAccountByEmail(email);

        if (staff == null) {
            model.addAttribute("error", "Không tìm thấy thông tin nhân viên!");
            return "staff/staff_profile";
        }

        model.addAttribute("title", "Staff Profile");
        model.addAttribute("staff", staff);

        return "staff/staff_profile";
    }
}