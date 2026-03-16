package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin/customers")
@RequiredArgsConstructor
public class AdminCustomerController {

    private final vn.edu.fpt.fashionstore.service.CustomerService customerService;

    // Kiểm tra quyền truy cập ADMIN
    private boolean isAdmin(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        return "Admin".equals(role);
    }

    // Trang quản lý khách hàng chính
    @GetMapping("")
    public String customers(
            HttpSession session,
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        // Lấy danh sách khách hàng
        Page<vn.edu.fpt.fashionstore.entity.Customer> customers;
        if (search != null && !search.trim().isEmpty()) {
            customers = customerService.searchCustomers(search, page, size);
        } else {
            customers = customerService.getAllCustomers(page, size);
        }

        // Lấy chi tiết cho từng khách hàng (bao gồm số đơn hàng, tổng chi tiêu, trạng thái)
        Map<Long, Map<String, Object>> customerDetailsMap = new java.util.HashMap<>();
        for (vn.edu.fpt.fashionstore.entity.Customer customer : customers.getContent()) {
            Map<String, Object> details = customerService.getCustomerDetails(customer.getCustomerId());
            customerDetailsMap.put(customer.getCustomerId(), details);
        }

        // Thêm dữ liệu vào model
        model.addAttribute("customers", customers);
        model.addAttribute("customerDetails", customerDetailsMap);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", customers.getTotalPages());
        model.addAttribute("totalItems", customers.getTotalElements());
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("title", "Customer Management");

        return "admin/admincustomer";
    }

    // Xem chi tiết khách hàng
    @GetMapping("/details/{id}")
    public String customerDetails(@PathVariable Long id, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        Map<String, Object> customerDetails = customerService.getCustomerDetails(id);
        if (customerDetails.isEmpty()) {
            return "redirect:/admin/customers";
        }

        model.addAttribute("customerDetails", customerDetails);
        model.addAttribute("title", "Customer Details");
        return "admin/customer_details";
    }

    // Cập nhật trạng thái khách hàng
    @PostMapping("/update-status/{id}")
    public String updateCustomerStatus(
            @PathVariable Long id,
            @RequestParam String status,
            HttpSession session) {
        
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            boolean success = customerService.updateCustomerAccountStatus(id, status);
            if (success) {
                return "redirect:/admin/customers?success=Customer status updated successfully";
            } else {
                return "redirect:/admin/customers?error=Customer not found or has no account";
            }
        } catch (Exception e) {
            return "redirect:/admin/customers?error=Error updating customer status: " + e.getMessage();
        }
    }

    // API endpoint for AJAX search
    @GetMapping("/search")
    @ResponseBody
    public Page<vn.edu.fpt.fashionstore.entity.Customer> searchCustomersAjax(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        return customerService.searchCustomers(keyword, page, size);
    }

    // API endpoint to check if customer has account
    @GetMapping("/check-account/{id}")
    @ResponseBody
    public Map<String, Object> checkCustomerAccount(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return Map.of("hasAccount", false, "error", "Unauthorized");
        }
        
        try {
            Optional<vn.edu.fpt.fashionstore.entity.Customer> customerOpt = customerService.getCustomerById(id);
            if (customerOpt.isPresent()) {
                vn.edu.fpt.fashionstore.entity.Customer customer = customerOpt.get();
                return Map.of("hasAccount", customer.getAccount() != null);
            } else {
                return Map.of("hasAccount", false, "error", "Customer not found");
            }
        } catch (Exception e) {
            return Map.of("hasAccount", false, "error", e.getMessage());
        }
    }
}
