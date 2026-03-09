package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    // change-password endpoints removed for staff; only customers can change password now.

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