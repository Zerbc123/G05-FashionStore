package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.entity.SupportRequest;
import vn.edu.fpt.fashionstore.entity.SupportChat;
import vn.edu.fpt.fashionstore.entity.SenderType;
import vn.edu.fpt.fashionstore.service.SupportRequestService;
import vn.edu.fpt.fashionstore.service.SupportChatService;
import vn.edu.fpt.fashionstore.service.AccountService;
import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.entity.Customer;

import java.util.List;

@Controller
@RequestMapping("/customer/support")
@RequiredArgsConstructor
public class CustomerSupportController {

    private final SupportRequestService supportRequestService;
    private final SupportChatService supportChatService;
    private final AccountService accountService;
    private final ChatWebSocketController chatWebSocketController;

    // Kiểm tra quyền truy cập CUSTOMER
    private boolean isCustomer(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        return role != null && role.equals("Customer");
    }

    // Lấy thông tin customer đang đăng nhập
    private Customer getCurrentCustomer(HttpSession session) {
        String email = (String) session.getAttribute("user");
        if (email == null) return null;
        
        return accountService.findCustomerByEmail(email);
    }

    // Hiển thị trang tạo yêu cầu hỗ trợ
    @GetMapping
    public String supportPage(HttpSession session, Model model) {
        if (!isCustomer(session)) {
            return "redirect:/login";
        }

        Customer customer = getCurrentCustomer(session);
        if (customer == null) {
            return "redirect:/login";
        }

        model.addAttribute("title", "Hỗ trợ khách hàng");
        model.addAttribute("customer", customer);
        
        return "customer/support";
    }

    // Tạo yêu cầu hỗ trợ mới
    @PostMapping("/create")
    public String createSupportRequest(
            @RequestParam String title,
            @RequestParam String description,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isCustomer(session)) {
            return "redirect:/login";
        }

        try {
            Customer customer = getCurrentCustomer(session);
            if (customer == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy thông tin khách hàng!");
                return "redirect:/customer/support";
            }

            // Tạo support request mới
            SupportRequest supportRequest = new SupportRequest();
            supportRequest.setTitle(title);
            supportRequest.setDescription(description);
            supportRequest.setCustomerName(customer.getFullName());
            supportRequest.setCustomerEmail(customer.getEmail());

            supportRequestService.save(supportRequest);

            redirectAttributes.addFlashAttribute("success", "Gửi yêu cầu hỗ trợ thành công! Chúng tôi sẽ phản hồi sớm nhất.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi gửi yêu cầu: " + e.getMessage());
        }

        return "redirect:/customer/support";
    }

    // Hiển thị danh sách yêu cầu hỗ trợ của customer
    @GetMapping("/my-tickets")
    public String myTickets(HttpSession session, Model model) {
        if (!isCustomer(session)) {
            return "redirect:/login";
        }

        Customer customer = getCurrentCustomer(session);
        if (customer == null) {
            return "redirect:/login";
        }

        // Lấy danh sách support requests của customer này
        List<SupportRequest> myTickets = supportRequestService.findByCustomerEmail(customer.getEmail());
        
        model.addAttribute("title", "Yêu cầu hỗ trợ của tôi");
        model.addAttribute("myTickets", myTickets);
        
        return "customer/my_tickets";
    }

    // Xem chi tiết và chat cho một yêu cầu hỗ trợ
    @GetMapping("/view/{id}")
    public String viewTicket(@PathVariable Long id, HttpSession session, Model model) {
        if (!isCustomer(session)) {
            return "redirect:/login";
        }

        try {
            SupportRequest supportRequest = supportRequestService.findById(id);
            Customer customer = getCurrentCustomer(session);

            // Kiểm tra xem ticket có thuộc về customer này không
            if (!supportRequest.getCustomerEmail().equals(customer.getEmail())) {
                return "redirect:/customer/support/my-tickets?error=access_denied";
            }

            // Lấy tin nhắn chat
            List<SupportChat> chatMessages = supportChatService.getMessagesBySupportRequestId(id);
            
            // Đánh dấu tin nhắn của staff là đã đọc
            supportChatService.markStaffMessagesAsRead(id);

            model.addAttribute("title", "Chi tiết yêu cầu hỗ trợ");
            model.addAttribute("supportRequest", supportRequest);
            model.addAttribute("chatMessages", chatMessages);
            model.addAttribute("customer", customer);

            return "customer/ticket_detail";

        } catch (Exception e) {
            return "redirect:/customer/support/my-tickets?error=not_found";
        }
    }

    // Gửi tin nhắn từ customer
    @PostMapping("/send-message/{id}")
    public String sendMessage(
            @PathVariable Long id,
            @RequestParam String messageContent,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isCustomer(session)) {
            return "redirect:/login";
        }

        try {
            SupportRequest supportRequest = supportRequestService.findById(id);
            Customer customer = getCurrentCustomer(session);

            // Kiểm tra xem ticket có thuộc về customer này không
            if (!supportRequest.getCustomerEmail().equals(customer.getEmail())) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền gửi tin nhắn cho yêu cầu này!");
                return "redirect:/customer/support/my-tickets";
            }

            // Gửi tin nhắn và lấy tin nhắn vừa gửi
            SupportChat newMessage = supportChatService.sendCustomerMessage(id, customer.getFullName(), messageContent);

            // Broadcast tin nhắn mới đến tất cả clients qua WebSocket
            chatWebSocketController.broadcastToRoom(id, newMessage);

            redirectAttributes.addFlashAttribute("success", "Đã gửi tin nhắn thành công!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi gửi tin nhắn: " + e.getMessage());
        }

        return "redirect:/customer/support/view/" + id;
    }
}
