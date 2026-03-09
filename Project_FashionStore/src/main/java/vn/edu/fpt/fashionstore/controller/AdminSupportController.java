package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.entity.SupportRequest;
import vn.edu.fpt.fashionstore.entity.SupportStatus;
import vn.edu.fpt.fashionstore.service.SupportRequestService;
import vn.edu.fpt.fashionstore.repository.AccountRepository;
import java.util.List;


@Controller
@RequestMapping("/admin/support")
@RequiredArgsConstructor
public class AdminSupportController {

    private final SupportRequestService supportRequestService;

    private final AccountRepository accountRepository;

    // Kiểm tra quyền truy cập ADMIN hoặc STAFF
    private boolean hasAccess(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        return "ADMIN".equals(role) || "STAFF".equals(role);
    }

    // Kiểm tra quyền admin (cho các thao tác đặc biệt)
    private boolean isAdmin(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        return "ADMIN".equals(role);
    }

    @GetMapping({"", "/"})
    public String listSupportRequests(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) SupportStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            HttpSession session,
            Model model) {
        
        if (!hasAccess(session)) {
            return "redirect:/login";
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<SupportRequest> supportRequestPage;
        
        if (keyword != null && !keyword.trim().isEmpty() && status != null) {
            supportRequestPage = supportRequestService.findByKeywordAndStatus(keyword, status, pageable);
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            supportRequestPage = supportRequestService.findByKeyword(keyword, pageable);
        } else if (status != null) {
            supportRequestPage = supportRequestService.findByStatus(status, pageable);
        } else {
            supportRequestPage = supportRequestService.findAll(pageable);
        }
        
        model.addAttribute("title", "Quản lý Yêu cầu Hỗ trợ");
        model.addAttribute("supportRequestPage", supportRequestPage);
        model.addAttribute("supportRequests", supportRequestPage.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", supportRequestPage.getTotalPages());
        model.addAttribute("totalItems", supportRequestPage.getTotalElements());
        model.addAttribute("isAdmin", isAdmin(session));
        model.addAttribute("hasAccess", hasAccess(session));

        return "admin/support";
    }

    @GetMapping("/view/{id}")
    public String viewSupportRequest(@PathVariable Long id, HttpSession session, Model model) {

        if (!hasAccess(session)) {
            return "redirect:/login";
        }

        SupportRequest supportRequest = supportRequestService.findById(id);

        // chỉ lấy SUPPORT
        List<Account> staffList = accountRepository.findByRole_RoleName("Hỗ trợ khách hàng (Support)");

        model.addAttribute("supportRequest", supportRequest);
        model.addAttribute("staffList", staffList);
        model.addAttribute("isAdmin", isAdmin(session));
        model.addAttribute("hasAccess", hasAccess(session));

        return "admin/support_detail";
    }

    @PostMapping("/update-status/{id}")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam SupportStatus status,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        if (!hasAccess(session)) {
            return "redirect:/login";
        }
        
        try {
            supportRequestService.updateStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
        return "redirect:/admin/support";
    }

    @PostMapping("/delete/{id}")
    public String deleteSupportRequest(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        
        try {
            supportRequestService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa yêu cầu hỗ trợ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa yêu cầu: " + e.getMessage());
        }
        return "redirect:/admin/support";
    }

    @GetMapping("/create")
    public String createSupportForm(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        
        model.addAttribute("title", "Create Support Request");
        model.addAttribute("supportRequest", new SupportRequest());
        model.addAttribute("statuses", SupportStatus.values());
        return "admin/support_create";
    }

    @PostMapping("/create")
    public String createSupportRequest(
            @ModelAttribute SupportRequest supportRequest,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        
        try {
            supportRequestService.create(supportRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo yêu cầu hỗ trợ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tạo yêu cầu: " + e.getMessage());
        }
        return "redirect:/admin/support";
    }

    @PostMapping("/assign/{id}")
    public String assignStaff(
            @PathVariable Long id,
            @RequestParam Integer staffId,
            RedirectAttributes redirectAttributes) {

        supportRequestService.assignStaff(id, staffId);

        redirectAttributes.addFlashAttribute("successMessage", "Phân công staff thành công!");

        return "redirect:/admin/support/view/" + id;
    }


}
