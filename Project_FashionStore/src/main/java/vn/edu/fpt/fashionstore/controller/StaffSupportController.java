package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.entity.SupportRequest;
import vn.edu.fpt.fashionstore.entity.SupportStatus;
import vn.edu.fpt.fashionstore.entity.SupportChat;
import vn.edu.fpt.fashionstore.entity.SenderType;
import vn.edu.fpt.fashionstore.service.SupportRequestService;
import vn.edu.fpt.fashionstore.service.SupportChatService;
import vn.edu.fpt.fashionstore.service.AccountService;
import vn.edu.fpt.fashionstore.entity.Account;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Controller
@RequestMapping("/staff/support")
@RequiredArgsConstructor
public class StaffSupportController {

    private final SupportRequestService supportRequestService;
    private final SupportChatService supportChatService;
    private final AccountService accountService;
    private final ChatWebSocketController chatWebSocketController;

    // Kiểm tra quyền truy cập STAFF
    private boolean isStaff(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        return role != null &&
                (role.contains("Nhân viên bán hàng (Sale)")
                        || role.contains("Quản lý kho (Stock)")
                        || role.contains("Hỗ trợ khách hàng (Support)")
                        || role.contains("Quản lý cửa hàng (Manager)")
                        || role.equalsIgnoreCase("Admin"));
    }

    // Lấy ID của staff đang đăng nhập
    private Integer getCurrentStaffId(HttpSession session) {
        // Thử lấy userId từ session trước (nếu có)
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId != null) {
            return userId;
        }
        
        // Nếu không có, lấy từ email
        String email = (String) session.getAttribute("user");
        if (email == null) return null;
        
        Optional<Account> accountOpt = accountService.findByEmail(email);
        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();
            return account.getAccountId();
        }
        return null;
    }

    // Lấy tên của staff đang đăng nhập
    private String getCurrentStaffName(HttpSession session) {
        String email = (String) session.getAttribute("user");
        if (email == null) return null;
        
        Optional<Account> accountOpt = accountService.findByEmail(email);
        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();
            return account.getFullName();
        }
        return null;
    }

    @GetMapping
    public String staffSupportPage(
            @RequestParam(value = "status", required = false) String status,
            HttpSession session, Model model) {
        if (!isStaff(session)) {
            return "redirect:/login";
        }

        // Lấy danh sách support requests của staff hiện tại
        Integer currentStaffId = getCurrentStaffId(session);
        List<SupportRequest> assignedRequests = new ArrayList<>();
        
        // Thống kê
        long openTicketsCount = 0;
        long inProgressCount = 0;
        long resolvedTodayCount = 0;
        long avgResponseMinutes = 0;
        
        if (currentStaffId != null) {
            // Lấy danh sách theo filter
            if (status != null && !status.isEmpty()) {
                SupportStatus statusEnum = SupportStatus.valueOf(status);
                assignedRequests = supportRequestService.getRequestsByStaffAndStatus(currentStaffId, statusEnum);
            } else {
                assignedRequests = supportRequestService.getRequestsByStaff(currentStaffId);
            }
            
            // Lấy thống kê cho staff
            List<SupportRequest> openTickets = supportRequestService.getRequestsByStaffAndStatus(currentStaffId, SupportStatus.OPEN);
            List<SupportRequest> inProgressTickets = supportRequestService.getRequestsByStaffAndStatus(currentStaffId, SupportStatus.IN_PROGRESS);
            List<SupportRequest> resolvedToday = supportRequestService.getRequestsByStaffAndDate(currentStaffId, java.time.LocalDate.now());
            avgResponseMinutes = supportRequestService.getAverageResponseTimeForStaff(currentStaffId);
            
            openTicketsCount = openTickets.size();
            inProgressCount = inProgressTickets.size();
            resolvedTodayCount = resolvedToday.size();
        }

        model.addAttribute("title", "Customer Support Tickets");
        model.addAttribute("assignedRequests", assignedRequests);
        model.addAttribute("currentStaffId", currentStaffId);
        model.addAttribute("status", status);
        
        // Thêm thống kê vào model
        model.addAttribute("openTicketsCount", openTicketsCount);
        model.addAttribute("inProgressCount", inProgressCount);
        model.addAttribute("resolvedTodayCount", resolvedTodayCount);
        model.addAttribute("avgResponseMinutes", avgResponseMinutes);

        return "staff/staffsupport";
    }

    @GetMapping("/list")
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
        Integer currentStaffId = getCurrentStaffId(session);

        // Kiểm tra xem staff có được phân công cho request này không
        if (supportRequest.getAssignedStaffId() != null && 
            !supportRequest.getAssignedStaffId().equals(currentStaffId)) {
            return "redirect:/staff/support?error=not_assigned";
        }

        // Lấy tin nhắn chat cho support request này
        List<SupportChat> chatMessages = supportChatService.getMessagesBySupportRequestId(id);
        
        // Đánh dấu tin nhắn của customer là đã đọc
        supportChatService.markCustomerMessagesAsRead(id);
        
        model.addAttribute("title", "Chi tiết yêu cầu hỗ trợ");
        model.addAttribute("supportRequest", supportRequest);
        model.addAttribute("chatMessages", chatMessages);
        model.addAttribute("currentStaffId", currentStaffId);
        model.addAttribute("currentStaffName", getCurrentStaffName(session));
        
        return "staff/support_detail";
    }

    @PostMapping("/update-status/{id}")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam SupportStatus status,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String role = (String) session.getAttribute("userRole");

        if(role == null || !role.contains("Hỗ trợ khách hàng (Support)")){
            return "redirect:/staff/access-denied";
        }

        try {
            SupportRequest supportRequest = supportRequestService.findById(id);
            Integer currentStaffId = getCurrentStaffId(session);

            // Kiểm tra xem staff có được phân công cho request này không
            if (supportRequest.getAssignedStaffId() != null &&
                    !supportRequest.getAssignedStaffId().equals(currentStaffId)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền cập nhật yêu cầu này!");
                return "redirect:/staff/support";
            }

            supportRequestService.updateStatus(id, status);

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái thành công!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật trạng thái");
        }

        return "redirect:/staff/support/view/" + id;
    }

    @PostMapping("/send-message/{id}")
    public String sendMessage(
            @PathVariable Long id,
            @RequestParam String messageContent,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String role = (String) session.getAttribute("userRole");

        if(role == null || !role.contains("Hỗ trợ khách hàng (Support)")){
            return "redirect:/staff/access-denied";
        }

        try {
            // Lấy thông tin staff hiện tại
            Integer staffId = getCurrentStaffId(session);
            String staffName = getCurrentStaffName(session);

            // Gửi tin nhắn và lấy tin nhắn vừa gửi
            SupportChat newMessage = supportChatService.sendStaffMessage(id, staffId, staffName, messageContent);

            // Broadcast tin nhắn mới đến tất cả clients qua WebSocket
            chatWebSocketController.broadcastToRoom(id, newMessage);

            redirectAttributes.addFlashAttribute("success", "Đã gửi tin nhắn thành công!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi gửi tin nhắn: " + e.getMessage());
        }

        return "redirect:/staff/support/view/" + id;
    }

    // Staff không thể tạo và xóa yêu cầu, chỉ có thể xử lý
}
