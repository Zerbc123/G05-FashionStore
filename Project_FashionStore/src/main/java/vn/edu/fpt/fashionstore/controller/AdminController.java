package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.fashionstore.service.ReportService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.service.AccountService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ReportService reportService;
    @Autowired
    private AccountService accountService;

    // Kiểm tra quyền truy cập ADMIN
    private boolean isAdmin(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        return "Admin".equals(role);
    }

    // Trang Admin View chính
    @GetMapping("")
    public String adminView(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Admin Panel");
        return "admin/view_admin";
    }

    // Admin Dashboard
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Admin Dashboard");
        return "admin/admindashboard";
    }

    // Quản lý sản phẩm
    @GetMapping("/products")
    public String products(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Product Management");
        return "admin/adminproduct";
    }

    // Thêm sản phẩm mới
    @GetMapping("/products/add")
    public String addProduct(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Add New Product");
        return "admin/addproduct";
    }

    // Chỉnh sửa sản phẩm
    @GetMapping("/edit")
    public String editProduct(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Edit Product");
        return "admin/edit";
    }

    // Quản lý khách hàng
    @GetMapping("/customers")
    public String customers(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Customer Management");
        return "admin/admincustomer";
    }

    // Xem chi tiết khách hàng
    @GetMapping("/customers/details/{id}")
    public String customerDetails(@PathVariable String id, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Customer Details");
        model.addAttribute("customerId", id);
        return "profile";
    }

    // ======== ADMIN PROFILE ========

    @GetMapping("/profile")
    public String adminProfile(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/login";

        String email = (String) session.getAttribute("user");
        if (email == null) return "redirect:/login";

        var admin = accountService.getAccountByEmail(email);

        if (admin == null) {
            model.addAttribute("error", "Không tìm thấy thông tin quản trị viên!");
            return "admin/admin_profile";
        }

        model.addAttribute("title", "Admin Profile");
        model.addAttribute("admin", admin);

        return "admin/admin_profile";
    }

    // Chỉnh sửa khách hàng
    @GetMapping("/customers/edit/{id}")
    public String editCustomer(@PathVariable String id, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Edit Customer");
        model.addAttribute("customerId", id);
        return "editprofile";
    }

    // Quản lý nhân viên
    @GetMapping("/staff")
    public String staff(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Staff Management");
        return "admin/view_staff";
    }

    // Thêm nhân viên mới
    @GetMapping("/staff/add")
    public String addStaff(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Add New Staff");
        return "admin/add_new_staff";
    }

    // Xem chi tiết nhân viên
    @GetMapping("/staffdetails/{id}")
    public String staffDetails(@PathVariable String id, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Staff Details");
        model.addAttribute("staffId", id);
        return "admin/view_staff_details";
    }

    // Hủy/Xóa nhân viên
    @GetMapping("/cancel_staff")
    public String cancelStaff(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Cancel Staff");
        return "admin/cancel_staff";
    }

    // Báo cáo
    @GetMapping("/reports")
    public String reports(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Reports");
        return "admin/reports";
    }

    // Revenue Report
    @GetMapping("/reports/revenue")
    public String revenueReport(HttpSession session, Model model,
                                @RequestParam(required = false) String startDate,
                                @RequestParam(required = false) String endDate) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        LocalDate start = startDate != null && !startDate.isEmpty() ? 
            LocalDate.parse(startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")) : 
            LocalDate.now().minusDays(7);
        LocalDate end = endDate != null && !endDate.isEmpty() ? 
            LocalDate.parse(endDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")) : 
            LocalDate.now();

        model.addAttribute("title", "Revenue Report");
        model.addAttribute("report", reportService.getRevenueReport(start, end));
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        return "admin/revenue_report";
    }

    // Best Seller Report
    @GetMapping("/reports/best-seller")
    public String bestSellerReport(HttpSession session, Model model,
                                  @RequestParam(required = false) String startDate,
                                  @RequestParam(required = false) String endDate) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        LocalDate start = startDate != null ? 
            LocalDate.parse(startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")) : 
            LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null ? 
            LocalDate.parse(endDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")) : 
            LocalDate.now();

        model.addAttribute("title", "Best Seller Report");
        model.addAttribute("report", reportService.getBestSellerReport(start, end));
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        return "admin/best_seller_report";
    }

    // Best Seller Report Filter (POST)
    @PostMapping("/reports/best-seller")
    public String bestSellerReportFilter(HttpSession session, Model model,
                                        @RequestParam(required = false) String startDate,
                                        @RequestParam(required = false) String endDate) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        LocalDate start = startDate != null && !startDate.isEmpty() ? 
            LocalDate.parse(startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")) : 
            LocalDate.now().minusMonths(1);
        LocalDate end = endDate != null && !endDate.isEmpty() ? 
            LocalDate.parse(endDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")) : 
            LocalDate.now();

        model.addAttribute("title", "Best Seller Report");
        model.addAttribute("report", reportService.getBestSellerReport(start, end));
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        return "admin/best_seller_report";
    }

    // Product Report
    @GetMapping("/reports/product")
    public String productReport(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        model.addAttribute("title", "Product Report");
        model.addAttribute("report", reportService.getProductReport());
        return "admin/product_report";
    }

    // Customer Report
    @GetMapping("/reports/customer")
    public String customerReport(HttpSession session, Model model,
                                @RequestParam(required = false) String startDate,
                                @RequestParam(required = false) String endDate) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        LocalDate start = startDate != null ? 
            LocalDate.parse(startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")) : 
            LocalDate.now().minusMonths(6);
        LocalDate end = endDate != null ? 
            LocalDate.parse(endDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")) : 
            LocalDate.now();

        model.addAttribute("title", "Customer Report");
        model.addAttribute("report", reportService.getCustomerReport(start, end));
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        return "admin/customer_report";
    }

    // Customer Report Filter (POST)
    @PostMapping("/reports/customer")
    public String customerReportFilter(HttpSession session, Model model,
                                      @RequestParam(required = false) String startDate,
                                      @RequestParam(required = false) String endDate) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        LocalDate start = startDate != null && !startDate.isEmpty() ? 
            LocalDate.parse(startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")) : 
            LocalDate.now().minusMonths(6);
        LocalDate end = endDate != null && !endDate.isEmpty() ? 
            LocalDate.parse(endDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")) : 
            LocalDate.now();

        model.addAttribute("title", "Customer Report");
        model.addAttribute("report", reportService.getCustomerReport(start, end));
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        return "admin/customer_report";
    }

    // Inventory Report
    @GetMapping("/reports/inventory")
    public String inventoryReport(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        model.addAttribute("title", "Inventory Report");
        model.addAttribute("report", reportService.getInventoryReport());
        return "admin/inventory_report";
    }
}
