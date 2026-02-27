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
import vn.edu.fpt.fashionstore.service.AccountService;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    @Autowired
    private AccountService accountService;

    private boolean isCustomer(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        return "Customer".equals(role);
    }

    @GetMapping("/change-password")
    public String changePasswordPage(HttpSession session, Model model) {
        if (!isCustomer(session)) {
            return "redirect:/login";
        }
        return "change_password";
    }

    @PostMapping("/change-password")
    public String handleChangePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session,
            RedirectAttributes ra) {

        if (!isCustomer(session)) {
            return "redirect:/login";
        }

        String email = (String) session.getAttribute("user");
        if (email == null) {
            return "redirect:/login";
        }

        // 1. Kiểm tra khớp
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Mật khẩu xác nhận không khớp!");
            return "redirect:/customer/change-password";
        }

        // 2. Kiểm tra định dạng mật khẩu mới
        if (!vn.edu.fpt.fashionstore.util.PasswordUtils.isValid(newPassword)) {
            ra.addFlashAttribute("error", "Mật khẩu mới phải gồm 6 ký tự chữ và số, không chứa ký tự đặc biệt!");
            return "redirect:/customer/change-password";
        }

        // 3. Đổi mật khẩu
        boolean success = accountService.changePassword(email, currentPassword, newPassword);

        if (success) {
            ra.addFlashAttribute("success", "Đổi mật khẩu thành công!");
            return "redirect:/profile";
        } else {
            ra.addFlashAttribute("error", "Mật khẩu hiện tại không đúng!");
            return "redirect:/customer/change-password";
        }
    }
}