package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.entity.ReturnRequest;
import vn.edu.fpt.fashionstore.entity.ReturnStatus;
import vn.edu.fpt.fashionstore.service.ReturnRequestService;

import java.util.List;

@Controller
@RequestMapping("/admin/returns")
public class AdminReturnController {

    @Autowired
    private ReturnRequestService returnRequestService;

    // Hàm kiểm tra quyền Admin
    private boolean isAdmin(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        return "Admin".equals(role) || "Staff".equals(role);
    }

    // 1. Hiển thị danh sách yêu cầu đổi trả
    @GetMapping
    public String listReturnRequests(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/login";

        List<ReturnRequest> returnRequests = returnRequestService.getAllReturnRequestsForAdmin();
        model.addAttribute("returnRequests", returnRequests);
        return "admin/managereturns";
    }

    // 2. Xử lý cập nhật trạng thái (Duyệt / Từ chối / Hoàn tất)
    @PostMapping("/update/{id}")
    public String updateStatus(
            @PathVariable("id") Long returnId,
            @RequestParam("status") String status,
            @RequestParam(value = "adminNote", required = false) String adminNote,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAdmin(session)) return "redirect:/login";

        try {
            ReturnStatus newStatus = ReturnStatus.valueOf(status);
            returnRequestService.updateReturnStatus(returnId, newStatus, adminNote);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái yêu cầu trả hàng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật: " + e.getMessage());
        }

        return "redirect:/admin/returns";
    }
}