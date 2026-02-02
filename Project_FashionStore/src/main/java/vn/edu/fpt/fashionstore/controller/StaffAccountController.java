package vn.edu.fpt.fashionstore.controller;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.service.AccountService;

@Controller
@RequestMapping("/admin/staff")
public class StaffAccountController {

    private final AccountService accountService;

    public StaffAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // Danh sách staff
    @GetMapping
    public String listStaff(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String keyword,
            Model model) {
        
        Page<Account> staffPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            staffPage = accountService.searchStaff(keyword, page, size);
        } else {
            staffPage = accountService.getAllStaff(page, size);
        }
        
        model.addAttribute("staffPage", staffPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", staffPage.getTotalPages());
        model.addAttribute("totalItems", staffPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        
        return "admin/view_staff";
    }

    // Form tạo staff
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("account", new Account());
        model.addAttribute("roles", accountService.getAllRoles());
        return "admin/add_new_staff";
    }

    // Hỗ trợ đường dẫn cũ từ giao diện: /admin/staff/add
    @GetMapping("/add")
    public String createFormLegacy(Model model) {
        return createForm(model);
    }

    // Submit tạo staff
    @PostMapping("/create")
    public String createStaff(@ModelAttribute Account account,
                              @RequestParam(required = false) Integer roleId,
                              Model model) {
        try {
            accountService.createStaff(account, roleId);
            return "redirect:/admin/staff";
        } catch (RuntimeException ex) {
            model.addAttribute("account", account);
            model.addAttribute("roles", accountService.getAllRoles());
            model.addAttribute("error", ex.getMessage());
            return "admin/add_new_staff";
        }
    }

    // Khóa tài khoản
    @GetMapping("/lock/{id}")
    public String lock(@PathVariable Integer id) {
        accountService.lockAccount(id);
        return "redirect:/admin/staff";
    }

    // Mở khóa
    @GetMapping("/unlock/{id}")
    public String unlock(@PathVariable Integer id) {
        accountService.unlockAccount(id);
        return "redirect:/admin/staff";
    }

    // Xem chi tiết staff
    @GetMapping("/details/{id}")
    public String staffDetails(@PathVariable Integer id, Model model) {
        Account staff = accountService.getById(id);
        model.addAttribute("staff", staff);
        return "admin/view_staff_details";
    }

    // Hỗ trợ đường dẫn cũ từ giao diện: /admin/staffdetails/{id}
    @GetMapping("/staffdetails/{id}")
    public String staffDetailsLegacy(@PathVariable Integer id, Model model) {
        return staffDetails(id, model);
    }

    // Form chỉnh sửa staff
    @GetMapping("/edit/{id}")
    public String editStaffForm(@PathVariable Integer id, Model model) {
        Account staff = accountService.getById(id);
        model.addAttribute("staff", staff);
        model.addAttribute("roles", accountService.getAllRoles());
        return "admin/update_staff";
    }

    // Xử lý cập nhật staff
    @PostMapping("/edit/{id}")
    public String updateStaff(@PathVariable Integer id, @ModelAttribute Account staff, Model model) {
        try {
            staff.setAccountId(id); // Đảm bảo ID đúng
            accountService.updateStaff(staff);
            return "redirect:/admin/staff";
        } catch (RuntimeException ex) {
            model.addAttribute("staff", staff);
            model.addAttribute("roles", accountService.getAllRoles());
            model.addAttribute("error", ex.getMessage());
            return "admin/update_staff";
        }
    }

    // Gỡ (xóa) nhân viên
    @GetMapping("/delete/{id}")
    public String deleteStaff(@PathVariable Integer id) {
        accountService.deleteAccount(id);
        return "redirect:/admin/staff";
    }
}
