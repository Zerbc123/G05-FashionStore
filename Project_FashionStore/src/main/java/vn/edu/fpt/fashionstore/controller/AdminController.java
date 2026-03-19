package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import vn.edu.fpt.fashionstore.service.ReportService;
import vn.edu.fpt.fashionstore.repository.OrderRepository;
import vn.edu.fpt.fashionstore.service.CloudinaryService;
import vn.edu.fpt.fashionstore.service.ProductService;
import vn.edu.fpt.fashionstore.service.ProductVariantService;
import vn.edu.fpt.fashionstore.entity.OrderStatus;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import vn.edu.fpt.fashionstore.entity.ProductVariant;
import vn.edu.fpt.fashionstore.service.AccountService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ProductService productService;
    private final ProductVariantService productVariantService;
    private final CloudinaryService cloudinaryService;
    private final OrderRepository orderRepository;
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
        
        // Lấy dữ liệu từ database
        List<vn.edu.fpt.fashionstore.entity.Product> products = productService.getAllProductsWithVariants();
        List<vn.edu.fpt.fashionstore.entity.Order> orders = orderRepository.findAll();
        
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
            .filter(order -> OrderStatus.COMPLETED.equals(order.getStatus()))
            .mapToDouble(order -> order.getTotalAmount() != null ? order.getTotalAmount() : 0.0)
            .sum();
        
        // Đếm đơn hàng theo trạng thái
        long pendingOrders = orders.stream()
            .filter(order -> OrderStatus.PENDING.equals(order.getStatus()))
            .count();
        long processingOrders = orders.stream()
            .filter(order -> OrderStatus.SHIPPING.equals(order.getStatus()))
            .count();
        long completedOrders = orders.stream()
            .filter(order -> OrderStatus.COMPLETED.equals(order.getStatus()))
            .count();
        
        // Lấy 5 đơn hàng gần nhất
        List<vn.edu.fpt.fashionstore.entity.Order> recentOrders = orders.stream()
            .sorted((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate()))
            .limit(5)
            .collect(java.util.stream.Collectors.toList());
        
        // Tính doanh thu theo tháng (Revenue Overview)
        Map<Integer, Double> revenueByMonth = orders.stream()
            .filter(order -> OrderStatus.COMPLETED.equals(order.getStatus()))
            .collect(java.util.stream.Collectors.groupingBy(
                order -> order.getOrderDate().getMonthValue(),
                java.util.stream.Collectors.summingDouble(order -> order.getTotalAmount() != null ? order.getTotalAmount() : 0.0)
            ));
        
        // Tạo dữ liệu cho 12 tháng
        double[] monthlyRevenue = new double[12];
        for (int month = 1; month <= 12; month++) {
            monthlyRevenue[month - 1] = revenueByMonth.getOrDefault(month, 0.0);
        }
        
        // Tính doanh thu theo danh mục với số lượng (Sales by Category)
        Map<String, Long> salesByCategory = orders.stream()
            .filter(order -> OrderStatus.COMPLETED.equals(order.getStatus()))
            .flatMap(order -> order.getOrderItems() != null ? order.getOrderItems().stream() : java.util.stream.Stream.empty())
            .collect(java.util.stream.Collectors.groupingBy(
                item -> {
                    ProductVariant variant = item.getProductVariant();
                    if (variant != null && variant.getProduct() != null && variant.getProduct().getCategory() != null) {
                        return variant.getProduct().getCategory().getCategoryName();
                    }
                    return "Unknown";
                },
                java.util.stream.Collectors.summingLong(item -> item.getQuantity() != null ? item.getQuantity() : 0)
            ));
        
        // Chuyển đổi thành dữ liệu cho chart
        List<String> categoryNames = new ArrayList<>(salesByCategory.keySet());
        List<Long> categorySales = new ArrayList<>();
        for (String categoryName : categoryNames) {
            categorySales.add(salesByCategory.get(categoryName));
        }
        
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("processingOrders", processingOrders);
        model.addAttribute("completedOrders", completedOrders);
        model.addAttribute("recentOrders", recentOrders);
        model.addAttribute("salesByCategory", salesByCategory);
        model.addAttribute("monthlyRevenue", monthlyRevenue);
        model.addAttribute("categoryNames", categoryNames);
        model.addAttribute("categorySales", categorySales);
        model.addAttribute("title", "Admin Dashboard");
        
        // Add dynamic data for dashboard
        model.addAttribute("stats", reportService.getDashboardStats());
        model.addAttribute("recentOrders", reportService.getRecentOrders(5));
        model.addAttribute("charts", reportService.getDashboardCharts());
        
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
        List<vn.edu.fpt.fashionstore.entity.Product> products = productService.getAllProductsWithVariants();

        // Tính toán thống kê
        long totalProducts = products.size();
        long inStockCount = 0;
        long lowStockCount = 0;
        long outOfStockCount = 0;
        
        for (vn.edu.fpt.fashionstore.entity.Product product : products) {
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

    // Quản lý kho hàng
    @GetMapping("/inventory")
    public String inventory(HttpSession session, Model model,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "10") int size) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        
        // Create pageable request
        Pageable pageable = PageRequest.of(page, size, Sort.by("variantId").ascending());
        
        // Get product variants with pagination
        List<vn.edu.fpt.fashionstore.entity.Product> products = productService.getAllProductsWithVariants();
        
        // Flatten all variants and create a simple page
        List<vn.edu.fpt.fashionstore.entity.ProductVariant> allVariants = products.stream()
            .flatMap(p -> p.getVariants() != null ? p.getVariants().stream() : java.util.stream.Stream.empty())
            .collect(java.util.stream.Collectors.toList());
        
        // Calculate pagination manually
        int start = page * size;
        int end = Math.min(start + size, allVariants.size());
        List<vn.edu.fpt.fashionstore.entity.ProductVariant> pageVariants = 
            start < allVariants.size() ? allVariants.subList(start, end) : java.util.Collections.emptyList();
        
        // Calculate statistics
        long totalProducts = allVariants.size();
        long inStockCount = allVariants.stream().filter(v -> v.getStock() != null && v.getStock() > 20).count();
        long lowStockCount = allVariants.stream().filter(v -> v.getStock() != null && v.getStock() > 0 && v.getStock() <= 20).count();
        long outOfStockCount = allVariants.stream().filter(v -> v.getStock() == null || v.getStock() == 0).count();
        
        // Create stats object
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalProducts", totalProducts);
        stats.put("inStockCount", inStockCount);
        stats.put("lowStockCount", lowStockCount);
        stats.put("outOfStockCount", outOfStockCount);
        
        // Create a simple page object for the template
        java.util.Map<String, Object> productVariantPage = new java.util.HashMap<>();
        productVariantPage.put("totalElements", totalProducts);
        productVariantPage.put("totalPages", (int) Math.ceil((double) totalProducts / size));
        productVariantPage.put("currentPage", page);
        productVariantPage.put("size", size);
        
        model.addAttribute("title", "Inventory Management");
        model.addAttribute("stats", stats);
        model.addAttribute("productVariants", pageVariants);
        model.addAttribute("productVariantPage", productVariantPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", (int) Math.ceil((double) totalProducts / size));
        model.addAttribute("size", size);
        
        return "admin/admininventory";
    }

    // Cập nhật số lượng tồn kho
    @PostMapping("/inventory/update-stock")
    public String updateStock(@RequestParam("variantId") Integer variantId,
                            @RequestParam("newStock") Integer newStock,
                            HttpSession session,
                            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            // Validate input
            if (newStock < 0) {
                redirectAttributes.addFlashAttribute("error", "Số lượng tồn kho không thể âm!");
                return "redirect:/admin/inventory";
            }

            // Get existing variant
            java.util.Optional<vn.edu.fpt.fashionstore.entity.ProductVariant> variantOpt = 
                productVariantService.getVariantById(variantId);
            
            if (!variantOpt.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy sản phẩm!");
                return "redirect:/admin/inventory";
            }

            // Update stock
            vn.edu.fpt.fashionstore.entity.ProductVariant variant = variantOpt.get();
            variant.setStock(newStock);
            productVariantService.updateVariant(variantId, variant);

            redirectAttributes.addFlashAttribute("success", "Cập nhật số lượng tồn kho thành công!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi cập nhật: " + e.getMessage());
        }

        return "redirect:/admin/inventory";
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

    // ======== ADMIN PROFILE ========

    @GetMapping("/profile")
    public String adminProfile(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/login";

        String email = (String) session.getAttribute("user");
        if (email == null) return "redirect:/login";

        vn.edu.fpt.fashionstore.entity.Account admin = accountService.getAccountByEmail(email);

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
