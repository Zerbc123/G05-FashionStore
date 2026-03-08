package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.fashionstore.repository.OrderRepository;
import vn.edu.fpt.fashionstore.service.CloudinaryService;
import vn.edu.fpt.fashionstore.service.ProductService;

import java.util.Map;
import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.service.AccountService;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ProductService productService;
    private final CloudinaryService cloudinaryService;
    private final OrderRepository orderRepository;
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
        
        // Lấy dữ liệu từ database
        var products = productService.getAllProductsWithVariants();
        var orders = orderRepository.findAll();
        
        // Tính toán thống kê
        long totalProducts = products.size();
        long totalOrders = orders.size();
        long totalCustomers = orders.stream()
            .map(order -> order.getCustomer() != null ? order.getCustomer().getCustomerId() : null)
            .filter(customerId -> customerId != null)
            .distinct()
            .count();
        
        // Tính tổng doanh thu
        double totalRevenue = orders.stream()
            .filter(order -> "Completed".equals(order.getStatus()))
            .mapToDouble(order -> order.getTotalAmount() != null ? order.getTotalAmount() : 0.0)
            .sum();
        
        // Đếm đơn hàng theo trạng thái
        long pendingOrders = orders.stream()
            .filter(order -> "Pending".equals(order.getStatus()))
            .count();
        long processingOrders = orders.stream()
            .filter(order -> "Shipping".equals(order.getStatus()))
            .count();
        long completedOrders = orders.stream()
            .filter(order -> "Completed".equals(order.getStatus()))
            .count();
        
        // Lấy 5 đơn hàng gần nhất
        var recentOrders = orders.stream()
            .sorted((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate()))
            .limit(5)
            .collect(java.util.stream.Collectors.toList());
        
        // Tính doanh thu theo danh mục
        Map<String, Long> salesByCategory = products.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                p -> p.getCategory() != null ? p.getCategory().getCategoryName() : "Unknown",
                java.util.stream.Collectors.counting()
            ));
        
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("processingOrders", processingOrders);
        model.addAttribute("completedOrders", completedOrders);
        model.addAttribute("recentOrders", recentOrders);
        model.addAttribute("salesByCategory", salesByCategory);
        model.addAttribute("title", "Admin Dashboard");
        
        return "admin/admindashboard";
    }

    // Quản lý sản phẩm
    @GetMapping("/products")
    public String products(HttpSession session, Model model,
                       @RequestParam(required = false) String search,
                       @RequestParam(required = false) String category) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        // Lấy danh sách sản phẩm từ database
        var products = productService.getAllProductsWithVariants();

        // Tính toán thống kê
        long totalProducts = products.size();
        long inStockCount = 0;
        long lowStockCount = 0;
        long outOfStockCount = 0;
        
        for (var product : products) {
            if (product.getVariants() != null && !product.getVariants().isEmpty()) {
                int stock = product.getVariants().get(0).getStock();
                if (stock > 20) {
                    inStockCount++;
                } else if (stock > 0) {
                    lowStockCount++;
                } else {
                    outOfStockCount++;
                }
            } else {
                // Không có variant = hết hàng
                outOfStockCount++;
            }
        }
        
        model.addAttribute("products", products);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("inStockCount", inStockCount);
        model.addAttribute("lowStockCount", lowStockCount);
        model.addAttribute("outOfStockCount", outOfStockCount);
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("selectedCategory", category != null ? category : "Tất cả");
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
    public String editProduct(@RequestParam("id") Long productId, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        // Redirect to ProductController's edit endpoint
        return "redirect:/products/admin/edit?id=" + productId;
    }

    // Xem chi tiết đơn hàng
    @GetMapping("/orderdetails/{id}")
    public String orderDetails(@PathVariable String id, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Order Details");
        model.addAttribute("orderId", id);
        return "orderdetails";
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
}

    // password-change endpoints for admin have been removed per requirement
    // (only customers may change their password now).}
