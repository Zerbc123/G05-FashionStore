package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.fpt.fashionstore.repository.OrderRepository;
import vn.edu.fpt.fashionstore.service.ProductService;
import vn.edu.fpt.fashionstore.service.ReportService;
import vn.edu.fpt.fashionstore.entity.OrderStatus;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.entity.ProductVariant;
import vn.edu.fpt.fashionstore.service.AccountService;
import vn.edu.fpt.fashionstore.service.InventoryService;
import vn.edu.fpt.fashionstore.repository.CategorySizeRepository;
import vn.edu.fpt.fashionstore.repository.ColorRepository;
import vn.edu.fpt.fashionstore.service.CategoryService;
import java.util.Collections;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageImpl;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ProductService productService;
    private final OrderRepository orderRepository;
    private final ReportService reportService;
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AccountService accountService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ColorRepository colorRepository;

    @Autowired
    private CategorySizeRepository categorySizeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    @Transactional(readOnly = true)
    public String dashboard(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        
        // Lấy dữ liệu từ database
        List<vn.edu.fpt.fashionstore.entity.Product> products = productService.getAllProductsWithVariants();
        List<vn.edu.fpt.fashionstore.entity.Order> orders = orderRepository.findOrdersForAdmin();
        
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
            .filter(order -> OrderStatus.CONFIRMED.equals(order.getStatus()))
            .mapToDouble(order -> order.getTotalAmount() != null ? order.getTotalAmount() : 0.0)
            .sum();
        
        // Đếm đơn hàng theo trạng thái
        long pendingOrders = orders.stream()
            .filter(order -> OrderStatus.PENDING.equals(order.getStatus()))
            .count();
        long cancelledOrders = orders.stream()
            .filter(order -> OrderStatus.CANCELLED.equals(order.getStatus()))
            .count();
        long confirmedOrders = orders.stream()
            .filter(order -> OrderStatus.CONFIRMED.equals(order.getStatus()))
            .count();
        
        // Lấy 5 đơn hàng gần nhất
        List<vn.edu.fpt.fashionstore.entity.Order> recentOrders = orders.stream()
            .sorted((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate()))
            .limit(5)
            .collect(java.util.stream.Collectors.toList());
        
        // Tính doanh thu theo tháng (Revenue Overview)
        Map<Integer, Double> revenueByMonth = orders.stream()
            .filter(order -> OrderStatus.CONFIRMED.equals(order.getStatus()))
            .collect(java.util.stream.Collectors.groupingBy(
                order -> order.getOrderDate().getMonthValue(), // LocalDate.getMonthValue() returns 1-12
                java.util.stream.Collectors.summingDouble(order -> order.getTotalAmount() != null ? order.getTotalAmount() : 0.0)
            ));
        
        // Tạo dữ liệu cho 12 tháng
        double[] monthlyRevenue = new double[12];
        for (int month = 1; month <= 12; month++) {
            monthlyRevenue[month - 1] = revenueByMonth.getOrDefault(month, 0.0);
        }
        
        // Tính doanh thu theo danh mục với số lượng (Sales by Category)
        Map<String, Long> salesByCategory = orders.stream()
            .filter(order -> OrderStatus.CONFIRMED.equals(order.getStatus()))
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
        model.addAttribute("cancelledOrders", cancelledOrders);
        model.addAttribute("confirmedOrders", confirmedOrders);
        model.addAttribute("recentOrders", recentOrders);
        model.addAttribute("salesByCategory", salesByCategory);
        model.addAttribute("monthlyRevenue", monthlyRevenue);
        model.addAttribute("categoryNames", categoryNames);
        model.addAttribute("categorySales", categorySales);
        model.addAttribute("title", "Admin Dashboard");
        
        return "admin/admindashboard";
    }

    // Quản lý sản phẩm
    @GetMapping("/products")
    @Transactional(readOnly = true)
    public String products(HttpSession session, Model model,
                       @RequestParam(required = false) String search,
                       @RequestParam(required = false) String category,
                       @RequestParam(required = false) String stockStatus,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("productId").ascending());
        
        // Sử dụng service để lọc
        Page<vn.edu.fpt.fashionstore.entity.Product> productPage = productService.searchAndFilterProducts(search, null, category, null, null, stockStatus, null, null, pageable);

        // Lấy danh sách sản phẩm đầy đủ để tính thống kê
        List<vn.edu.fpt.fashionstore.entity.Product> allProducts = productService.getAllProductsWithVariants();
        long totalProducts = allProducts.size();
        long inStockCount = allProducts.stream().filter(p -> p.getTotalStock() > 20).count();
        long lowStockCount = allProducts.stream().filter(p -> p.getTotalStock() > 0 && p.getTotalStock() <= 20).count();
        long outOfStockCount = allProducts.stream().filter(p -> p.getTotalStock() == 0).count();
        
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("productPage", productPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalItems", productPage.getTotalElements());
        model.addAttribute("pageSize", size);
        
        // Stats
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("inStockCount", inStockCount);
        model.addAttribute("lowStockCount", lowStockCount);
        model.addAttribute("outOfStockCount", outOfStockCount);
        
        // Filters
        model.addAttribute("search", search);
        model.addAttribute("category", category);
        model.addAttribute("stockStatus", (stockStatus != null && !stockStatus.isEmpty()) ? stockStatus : "all");
        
        // DỰNG DANH MỤC CHO FILTER (Lỗi user báo: thiếu danh mục)
        model.addAttribute("categories", categoryService.getAllCategories().stream()
                .map(vn.edu.fpt.fashionstore.entity.Category::getCategoryName)
                .distinct()
                .collect(Collectors.toList()));
        
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
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("colors", colorRepository.findAll());
        model.addAttribute("sizes", categorySizeRepository.findAll());
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
    @Transactional(readOnly = true)
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

    // Quản lý kho
    @GetMapping("/inventory")
    public String inventory(@RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size,
                         HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/login";
        
        logger.info("Admin accessing inventory page: page={}, size={}", page, size);
        
        // Fetch paginated data from database
        Pageable pageable = PageRequest.of(page, size);
        Page<InventoryService.ProductInventoryDTO> productPage;
        InventoryService.InventoryStats stats;
        
        try {
            productPage = inventoryService.getAllProductsGrouped(pageable);
            stats = inventoryService.getInventoryStats();
            
            logger.info("Admin inventory - Total products found: {}", productPage.getTotalElements());
            logger.info("Admin inventory - Page {} of {}, total pages: {}", page, productPage.getTotalPages());
            
            // Add message if no data found
            if (productPage.getTotalElements() == 0) {
                model.addAttribute("emptyMessage", "Không có sản phẩm nào trong kho. Vui lòng thêm sản phẩm trước.");
                logger.warn("Admin inventory - No products found in database");
            }
        } catch (Exception e) {
            logger.error("Admin inventory - Error fetching data: {}", e.getMessage(), e);
            // Create empty page to avoid template errors
            productPage = Page.empty(pageable);
            stats = new InventoryService.InventoryStats(0, 0, 0, 0);
            model.addAttribute("errorMessage", "Có lỗi xảy ra khi tải dữ liệu: " + e.getMessage());
        }
        
        // Get all categories for dropdown
        List<String> categories = inventoryService.getAllCategories();
        
        model.addAttribute("title", "Inventory Management");
        model.addAttribute("productPage", productPage);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("stats", stats);
        model.addAttribute("categories", categories);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalElements", productPage.getTotalElements());
        
        // Add filter attributes for pagination consistency
        model.addAttribute("search", null);
        model.addAttribute("category", "all");
        model.addAttribute("stockStatus", "all");
        
        logger.info("Returning admin inventory view with {} items, total pages: {}", 
                   productPage.getContent().size(), productPage.getTotalPages());
        
        return "admin/admininventory";
    }

    @PostMapping("/inventory/update-stock")
    public String updateStock(@RequestParam Integer variantId, 
                            @RequestParam Integer newStock,
                            HttpSession session, 
                            RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) return "redirect:/login";
        
        try {
            inventoryService.updateStock(variantId, newStock);
            redirectAttributes.addFlashAttribute("success", "Cập nhật tồn kho thành công!");
            logger.info("Admin updated stock for variant {} to {}", variantId, newStock);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi cập nhật tồn kho: " + e.getMessage());
            logger.error("Error updating stock for variant {}: {}", variantId, e.getMessage());
        }
        
        return "redirect:/admin/inventory";
    }

    @GetMapping("/inventory/filter")
    public String filterInventory(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 @RequestParam(required = false) String search,
                                 @RequestParam(required = false) String category,
                                 @RequestParam(required = false) String stockStatus,
                                 HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/login";
        
        logger.info("=== Admin Inventory Filter Request ===");
        logger.info("Page: {}, Size: {}", page, size);
        logger.info("Search: '{}'", search);
        logger.info("Category: '{}'", category);
        logger.info("Stock Status: '{}'", stockStatus);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<InventoryService.ProductInventoryDTO> productPage;
        
        try {
            // Get all products grouped
            Page<InventoryService.ProductInventoryDTO> allProductsPage = inventoryService.getAllProductsGrouped(PageRequest.of(0, Integer.MAX_VALUE));
            List<InventoryService.ProductInventoryDTO> allProducts = allProductsPage.getContent();
            
            // Apply filters sequentially
            List<InventoryService.ProductInventoryDTO> filteredProducts = allProducts;
            
            // Apply search filter if provided
            if (search != null && !search.trim().isEmpty()) {
                logger.info("Applying search filter for: '{}'", search.trim());
                String searchTrim = search.trim().toLowerCase();
                filteredProducts = filteredProducts.stream()
                    .filter(product -> product.getProductName() != null &&
                        product.getProductName().toLowerCase().contains(searchTrim))
                    .collect(Collectors.toList());
            }
            
            // Apply category filter if provided
            if (category != null && !category.equals("all")) {
                logger.info("Applying category filter for: '{}'", category);
                final String categoryFilter = category;
                filteredProducts = filteredProducts.stream()
                    .filter(product -> product.getCategoryName() != null &&
                        product.getCategoryName().equalsIgnoreCase(categoryFilter))
                    .collect(Collectors.toList());
            }
            
            // Apply stock status filter if provided
            if (stockStatus != null && !stockStatus.equals("all")) {
                logger.info("Applying stock status filter for: '{}'", stockStatus);
                final String stockStatusFilter = stockStatus;
                filteredProducts = filteredProducts.stream()
                    .filter(product -> product.getStatus() != null &&
                        product.getStatus().equals(stockStatusFilter))
                    .collect(Collectors.toList());
            }
            
            // Apply pagination
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filteredProducts.size());
            
            List<InventoryService.ProductInventoryDTO> pageContent = start < filteredProducts.size() ? 
                filteredProducts.subList(start, end) : Collections.emptyList();
            
            productPage = new PageImpl<>(pageContent, pageable, filteredProducts.size());
            
            logger.info("Filter result: {} items found (from {} total after filtering)", 
                       productPage.getTotalElements(), filteredProducts.size());
            
        } catch (Exception e) {
            logger.error("Error in filterInventory: {}", e.getMessage(), e);
            // Fallback to all products
            productPage = inventoryService.getAllProductsGrouped(pageable);
        }
        
        InventoryService.InventoryStats stats = inventoryService.getInventoryStats();
        
        // Get all categories for dropdown
        List<String> categories = inventoryService.getAllCategories();
        
        model.addAttribute("title", "Inventory Management");
        model.addAttribute("productPage", productPage);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("stats", stats);
        model.addAttribute("categories", categories);
        model.addAttribute("search", search);
        model.addAttribute("category", category);
        model.addAttribute("stockStatus", stockStatus);
        
        // Only add pagination attributes if there are results
        if (productPage.getTotalElements() > 0) {
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", productPage.getTotalPages());
            model.addAttribute("totalElements", productPage.getTotalElements());
            model.addAttribute("pageSize", size);
        } else {
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0);
            model.addAttribute("pageSize", size);
        }
        
        logger.info("Returning admin inventory view with {} items, total pages: {}", 
                   productPage.getContent().size(), productPage.getTotalPages());
        
        return "admin/admininventory";
    }

    // Revenue Report
    @GetMapping("/reports/revenue")
    public String revenueReport(HttpSession session, Model model,
                               @RequestParam(required = false) String startDate,
                               @RequestParam(required = false) String endDate) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            java.time.LocalDate start = startDate != null ? 
                java.time.LocalDate.parse(startDate) : java.time.LocalDate.now().minusMonths(1);
            java.time.LocalDate end = endDate != null ? 
                java.time.LocalDate.parse(endDate) : java.time.LocalDate.now();

            Map<String, Object> reportData = reportService.getRevenueReport(start, end);
            model.addAttribute("report", reportData);
            model.addAttribute("startDate", start);
            model.addAttribute("endDate", end);
            model.addAttribute("title", "Revenue Report");
            
            return "admin/revenue_report";
        } catch (Exception e) {
            model.addAttribute("error", "Error generating report: " + e.getMessage());
            return "admin/revenue_report";
        }
    }

    // Best Seller Report
    @GetMapping("/reports/best-seller")
    public String bestSellerReport(HttpSession session, Model model,
                                  @RequestParam(required = false) String startDate,
                                  @RequestParam(required = false) String endDate) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            java.time.LocalDate start = startDate != null ? 
                java.time.LocalDate.parse(startDate) : java.time.LocalDate.now().minusMonths(1);
            java.time.LocalDate end = endDate != null ? 
                java.time.LocalDate.parse(endDate) : java.time.LocalDate.now();

            Map<String, Object> reportData = reportService.getBestSellerReport(start, end);
            model.addAttribute("report", reportData);
            model.addAttribute("startDate", start);
            model.addAttribute("endDate", end);
            model.addAttribute("title", "Best Seller Report");
            
            return "admin/best_seller_report";
        } catch (Exception e) {
            model.addAttribute("error", "Error generating report: " + e.getMessage());
            return "admin/best_seller_report";
        }
    }

    // Inventory Report
    @GetMapping("/reports/inventory")
    public String inventoryReport(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            Map<String, Object> reportData = reportService.getInventoryReport();
            model.addAttribute("report", reportData);
            model.addAttribute("title", "Inventory Report");
            
            return "admin/inventory_report";
        } catch (Exception e) {
            model.addAttribute("error", "Error generating report: " + e.getMessage());
            return "admin/inventory_report";
        }
    }

    // Product Report
    @GetMapping("/reports/product")
    public String productReport(HttpSession session, Model model,
                               @RequestParam(required = false) String startDate,
                               @RequestParam(required = false) String endDate) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            Map<String, Object> reportData = reportService.getProductReport();
            model.addAttribute("report", reportData);
            model.addAttribute("title", "Product Report");
            
            return "admin/product_report";
        } catch (Exception e) {
            model.addAttribute("error", "Error generating report: " + e.getMessage());
            return "admin/product_report";
        }
    }

    // Customer Report
    @GetMapping("/reports/customer")
    public String customerReport(HttpSession session, Model model,
                               @RequestParam(required = false) String startDate,
                               @RequestParam(required = false) String endDate) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try {
            java.time.LocalDate start = startDate != null ? 
                java.time.LocalDate.parse(startDate) : java.time.LocalDate.now().minusMonths(1);
            java.time.LocalDate end = endDate != null ? 
                java.time.LocalDate.parse(endDate) : java.time.LocalDate.now();

            Map<String, Object> reportData = reportService.getCustomerReport(start, end);
            model.addAttribute("report", reportData);
            model.addAttribute("startDate", start);
            model.addAttribute("endDate", end);
            model.addAttribute("title", "Customer Report");
            
            return "admin/customer_report";
        } catch (Exception e) {
            model.addAttribute("error", "Error generating report: " + e.getMessage());
            return "admin/customer_report";
        }
    }

}
