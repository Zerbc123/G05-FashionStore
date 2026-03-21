package vn.edu.fpt.fashionstore.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.entity.Role;
import vn.edu.fpt.fashionstore.service.AccountService;

import java.util.HashMap;
import java.util.Map;

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
            @RequestParam(required = false) String error,
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
        
        // Thêm thông báo lỗi
        if (error != null) {
            model.addAttribute("errorMessage", error);
        }
        
        return "admin/view_staff";
    }

    // Form tạo staff
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("account", new Account());
        model.addAttribute("roles", accountService.getStaffRoles());
        return "admin/add_new_staff";
    }

    // Hỗ trợ đường dẫn cũ từ giao diện: /admin/staff/add
    @GetMapping("/add")
    public String createFormLegacy(Model model) {
        return createForm(model);
    }

    // Submit tạo staff
    @PostMapping("/create")
    public String createStaff(@Valid @ModelAttribute Account account,
                              @RequestParam(required = false) Integer roleId,
                              BindingResult bindingResult,
                              Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("account", account);
            model.addAttribute("roles", accountService.getStaffRoles());
            model.addAttribute("error", "Vui lòng kiểm tra lại thông tin nhập vào");
            return "admin/add_new_staff";
        }
        
        try {
            accountService.createStaff(account, roleId);
            return "redirect:/admin/staff";
        } catch (RuntimeException ex) {
            model.addAttribute("account", account);
            model.addAttribute("roles", accountService.getStaffRoles());
            model.addAttribute("error", ex.getMessage());
            return "admin/add_new_staff";
        }
    }

    // Khóa tài khoản
    @GetMapping("/lock/{id}")
    public String lock(@PathVariable Integer id) {
        if (id == null || id <= 0) {
            return "redirect:/admin/staff?error=Invalid staff ID";
        }
        accountService.lockAccount(id);
        return "redirect:/admin/staff";
    }

    // Mở khóa
    @GetMapping("/unlock/{id}")
    public String unlock(@PathVariable Integer id) {
        if (id == null || id <= 0) {
            return "redirect:/admin/staff?error=Invalid staff ID";
        }
        accountService.unlockAccount(id);
        return "redirect:/admin/staff";
    }

    // Đặt trạng thái nghỉ phép
    @GetMapping("/leave/{id}")
    public String leave(@PathVariable Integer id) {
        if (id == null || id <= 0) {
            return "redirect:/admin/staff?error=Invalid staff ID";
        }
        accountService.leaveAccount(id);
        return "redirect:/admin/staff";
    }

    // Xem chi tiết staff
    @GetMapping("/details/{id}")
    public String staffDetails(@PathVariable Integer id, Model model) {
        if (id == null || id <= 0) {
            return "redirect:/admin/staff?error=Invalid staff ID";
        }
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
        if (id == null || id <= 0) {
            return "redirect:/admin/staff?error=Invalid staff ID";
        }
        
        try {
            Account staff = accountService.getById(id);
            model.addAttribute("staff", staff);
            model.addAttribute("roles", accountService.getStaffRoles());
            return "admin/update_staff";
        } catch (RuntimeException ex) {
            return "redirect:/admin/staff?error=" + ex.getMessage();
        }
    }

    // Xử lý cập nhật staff
    @PostMapping("/edit/{id}")
    public String updateStaff(@PathVariable Integer id, 
                          @Valid @ModelAttribute Account staff, 
                          BindingResult bindingResult,
                          @RequestParam(required = false) Integer roleId,
                          Model model) {
        if (id == null || id <= 0) {
            return "redirect:/admin/staff?error=Invalid staff ID";
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("staff", staff);
            model.addAttribute("roles", accountService.getStaffRoles());
            model.addAttribute("error", "Vui lòng kiểm tra lại thông tin nhập vào");
            return "admin/update_staff";
        }
        
        try {
            staff.setAccountId(id); // Đảm bảo ID đúng
            
            accountService.updateStaffInfo(staff, roleId);
            return "redirect:/admin/staff";
        } catch (RuntimeException ex) {
            model.addAttribute("staff", staff);
            model.addAttribute("roles", accountService.getStaffRoles());
            model.addAttribute("error", ex.getMessage());
            return "admin/update_staff";
        }
    }

    // Form chỉnh sửa thông tin nhân viên
    @GetMapping("/edit-info/{id}")
    public String editStaffInfoForm(@PathVariable Integer id, Model model) {
        if (id == null || id <= 0) {
            return "redirect:/admin/staff?error=Invalid staff ID";
        }
        
        try {
            Account staff = accountService.getById(id);
            model.addAttribute("staff", staff);
            model.addAttribute("roles", accountService.getStaffRoles());
            return "admin/edit_staff_info";
        } catch (RuntimeException ex) {
            return "redirect:/admin/staff?error=" + ex.getMessage();
        }
    }

    // Xử lý cập nhật thông tin nhân viên
    @PostMapping("/edit-info/{id}")
    public String updateStaffInfo(@PathVariable Integer id, 
                                  @Valid @ModelAttribute Account staff, 
                                  BindingResult bindingResult,
                                  @RequestParam(required = false) Integer roleId,
                                  Model model) {
        if (id == null || id <= 0) {
            return "redirect:/admin/staff?error=Invalid staff ID";
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("staff", staff);
            model.addAttribute("roles", accountService.getStaffRoles());
            model.addAttribute("error", "Vui lòng kiểm tra lại thông tin nhập vào");
            return "admin/edit_staff_info";
        }
        
        try {
            Account existingStaff = accountService.getById(id);
            staff.setAccountId(id); // Đảm bảo ID đúng
            
            // Giữ lại mật khẩu cũ nếu không được nhập mới
            if (staff.getPassword() == null || staff.getPassword().trim().isEmpty()) {
                staff.setPassword(existingStaff.getPassword());
            }
            
            accountService.updateStaffInfo(staff, roleId);
            return "redirect:/admin/staff";
        } catch (RuntimeException ex) {
            model.addAttribute("staff", staff);
            model.addAttribute("roles", accountService.getStaffRoles());
            model.addAttribute("error", ex.getMessage());
            return "admin/edit_staff_info";
        }
    }

    // Gỡ (xóa) nhân viên
    @GetMapping("/delete/{id}")
    public String deleteStaff(@PathVariable Integer id) {
        if (id == null || id <= 0) {
            return "redirect:/admin/staff?error=Invalid staff ID";
        }
        accountService.deleteAccount(id);
        return "redirect:/admin/staff";
    }

    // Trang thùng rác - hiển thị nhân viên đã bị xóa
    @GetMapping("/trash")
    public String viewTrash(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String error,
            Model model) {
        
        Page<Account> deletedStaffPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            deletedStaffPage = accountService.searchDeletedStaff(keyword, page, size);
        } else {
            deletedStaffPage = accountService.getDeletedStaff(page, size);
        }
        
        model.addAttribute("deletedStaffPage", deletedStaffPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", deletedStaffPage.getTotalPages());
        model.addAttribute("totalItems", deletedStaffPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        
        // Thêm thông báo lỗi
        if (error != null) {
            model.addAttribute("errorMessage", error);
        }
        
        return "admin/trash";
    }

    // Khôi phục nhân viên từ thùng rác
    @GetMapping("/restore/{id}")
    public String restoreStaff(@PathVariable Integer id) {
        if (id == null || id <= 0) {
            return "redirect:/admin/staff/trash?error=Invalid staff ID";
        }
        
        try {
            accountService.restoreAccount(id);
            return "redirect:/admin/staff/trash";
        } catch (RuntimeException ex) {
            return "redirect:/admin/staff/trash?error=" + ex.getMessage();
        }
    }

    // Xóa vĩnh viễn nhân viên khỏi database
    @GetMapping("/hard-delete/{id}")
    public String hardDeleteStaff(@PathVariable Integer id,
                                  RedirectAttributes redirectAttributes) {

        try {

            if (accountService.isStaffAssigned(id)) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Không thể xóa nhân viên này vì đang được phân công xử lý yêu cầu khách hàng!"
                );
                return "redirect:/admin/staff/trash";
            }

            accountService.hardDeleteAccount(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Xóa vĩnh viễn tài khoản thành công!"
            );

        } catch (RuntimeException e) {
            
            // Handle specific FK constraint violation
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("đang được phân công xử lý yêu cầu hỗ trợ")) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        errorMessage
                );
            } else {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Không thể xóa vì tài khoản đang được sử dụng trong hệ thống!"
                );
            }
        }

        return "redirect:/admin/staff/trash";
    }

    // Check phone duplicate (AJAX endpoint)
    @GetMapping("/check-phone-duplicate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkPhoneDuplicate(
            @RequestParam String phone,
            @RequestParam(required = false) Integer accountId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean exists;
            if (accountId != null) {
                // Check for update (exclude current account)
                exists = accountService.existsByPhoneAndAccountIdNot(phone, accountId);
            } else {
                // Check for create
                exists = accountService.existsByPhone(phone);
            }
            
            response.put("exists", exists);
            response.put("message", exists ? "Số điện thoại đã tồn tại" : "Số điện thoại có thể sử dụng");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("exists", false);
            response.put("message", "Lỗi khi kiểm tra số điện thoại");
            return ResponseEntity.badRequest().body(response);
        }
    }
}
