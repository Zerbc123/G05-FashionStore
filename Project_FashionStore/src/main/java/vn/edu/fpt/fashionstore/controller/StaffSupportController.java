package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.entity.SupportRequest;
import vn.edu.fpt.fashionstore.entity.SupportStatus;
import vn.edu.fpt.fashionstore.service.SupportRequestService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Controller
@RequestMapping("/staff/support")
@RequiredArgsConstructor
public class StaffSupportController {

    private final SupportRequestService supportRequestService;

    // Kiểm tra quyền truy cập STAFF
    private boolean isStaff(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        return "STAFF".equals(role) || "ADMIN".equals(role);
    }

    @GetMapping({"", "/"})
    public String listSupportRequests(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) SupportStatus status,
            HttpSession session,
            Model model) {
        
        if (!isStaff(session)) {
            return "redirect:/login";
        }

        Pageable pageable = PageRequest.of(0, 50);
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

        List<SupportRequest> supportRequests = supportRequestPage.getContent();
        
        model.addAttribute("title", "Yêu cầu Hỗ trợ");
        model.addAttribute("supportRequests", supportRequests);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("isAdmin", false); // Staff không có quyền admin
        model.addAttribute("hasAccess", true);

        return "admin/support";
    }

    @GetMapping("/view/{id}")
    public String viewSupportRequest(@PathVariable Long id, HttpSession session, Model model) {
        if (!isStaff(session)) {
            return "redirect:/login";
        }
        
        SupportRequest supportRequest = supportRequestService.findById(id);
        model.addAttribute("title", "Chi tiết yêu cầu hỗ trợ");
        model.addAttribute("supportRequest", supportRequest);
        model.addAttribute("isAdmin", false); // Staff không có quyền admin
        model.addAttribute("hasAccess", true); // Staff có quyền truy cập
        return "admin/support_detail";
    }

    @PostMapping("/update-status/{id}")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam SupportStatus status,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        if (!isStaff(session)) {
            return "redirect:/login";
        }
        
        try {
            supportRequestService.updateStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
        return "redirect:/staff/support";
    }

    // Staff không thể tạo và xóa yêu cầu, chỉ có thể xử lý
}
