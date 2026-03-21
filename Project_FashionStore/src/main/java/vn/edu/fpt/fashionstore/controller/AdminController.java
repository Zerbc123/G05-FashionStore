package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import vn.edu.fpt.fashionstore.repository.OrderRepository;
import vn.edu.fpt.fashionstore.service.ProductService;
import vn.edu.fpt.fashionstore.service.ProductVariantService;
import vn.edu.fpt.fashionstore.service.ReportService;
import vn.edu.fpt.fashionstore.entity.OrderStatus;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.entity.ProductVariant;
import vn.edu.fpt.fashionstore.service.AccountService;
import vn.edu.fpt.fashionstore.service.InventoryService;

import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageImpl;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ProductService productService;
    private final ProductVariantService productVariantService;
    private final OrderRepository orderRepository;
    private final ReportService reportService;
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AccountService accountService;

    @Autowired
    private InventoryService inventoryService;

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
        
        return "admin/admindashboard";
    }

    // Quản lý sản phẩm
    @GetMapping("/products")
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public String inventory(HttpSession session, Model model,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "10") int size) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        
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

    // Revenue Report
    @GetMapping("/reports/revenue")
    public String revenueReport(HttpSession session, Model model,
                               @RequestParam(required = false) String startDate,
                               @RequestParam(required = false) String endDate) {
        if (!isAdmin(session)) {
            return "redirect:/login";
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
        Page<ProductVariant> productVariantPage;
        InventoryService.InventoryStats stats;
        
        try {
            productVariantPage = inventoryService.getAllProductVariantsWithProductAndCategory(pageable);
            stats = inventoryService.getInventoryStats();
            
            logger.info("Admin inventory - Total variants found: {}", productVariantPage.getTotalElements());
            logger.info("Admin inventory - Page {} of {}, total pages: {}", page, productVariantPage.getTotalPages());
            
            // Add message if no data found
            if (productVariantPage.getTotalElements() == 0) {
                model.addAttribute("emptyMessage", "Không có sản phẩm nào trong kho. Vui lòng thêm sản phẩm trước.");
                logger.warn("Admin inventory - No products found in database");
            }
        } catch (Exception e) {
            logger.error("Admin inventory - Error fetching data: {}", e.getMessage(), e);
            // Create empty page to avoid template errors
            productVariantPage = Page.empty(pageable);
            stats = new InventoryService.InventoryStats(0, 0, 0, 0);
            model.addAttribute("errorMessage", "Có lỗi xảy ra khi tải dữ liệu: " + e.getMessage());
        }
        
        // Get all categories for dropdown
        List<String> categories = inventoryService.getAllCategories();
        
        model.addAttribute("title", "Inventory Management");
        model.addAttribute("productVariantPage", productVariantPage);
        model.addAttribute("productVariants", productVariantPage.getContent());
        model.addAttribute("stats", stats);
        model.addAttribute("categories", categories);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", productVariantPage.getTotalPages());
        model.addAttribute("totalElements", productVariantPage.getTotalElements());
        
        // Add filter attributes for pagination consistency
        model.addAttribute("search", null);
        model.addAttribute("category", "all");
        model.addAttribute("stockStatus", "all");
        
        logger.info("Returning admin inventory view with {} items, total pages: {}", 
                   productVariantPage.getContent().size(), productVariantPage.getTotalPages());
        
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
        Page<ProductVariant> productVariantPage;
        
        try {
            // Get all variants with JOIN FETCH to avoid lazy loading
            List<ProductVariant> allVariants = inventoryService.getAllVariantsWithProductAndCategory();
            
            // Apply filters sequentially
            List<ProductVariant> filteredVariants = allVariants;
            
            // Apply search filter if provided
            if (search != null && !search.trim().isEmpty()) {
                logger.info("Applying search filter for: '{}'", search.trim());
                String searchTrim = search.trim().toLowerCase();
                filteredVariants = filteredVariants.stream()
                    .filter(variant -> variant.getProduct() != null &&
                        variant.getProduct().getProductName() != null &&
                        (variant.getProduct().getProductName().toLowerCase().contains(searchTrim) ||
                         (variant.getProduct().getDescription() != null &&
                          variant.getProduct().getDescription().toLowerCase().contains(searchTrim))))
                    .collect(Collectors.toList());
            }
            
            // Apply category filter if provided
            if (category != null && !category.equals("all")) {
                logger.info("Applying category filter for: '{}'", category);
                final String categoryFilter = category;
                filteredVariants = filteredVariants.stream()
                    .filter(variant -> variant.getProduct() != null && 
                        variant.getProduct().getCategory() != null &&
                        variant.getProduct().getCategory().getCategoryName() != null &&
                        variant.getProduct().getCategory().getCategoryName().equalsIgnoreCase(categoryFilter))
                    .collect(Collectors.toList());
            }
            
            // Apply stock status filter if provided
            if (stockStatus != null && !stockStatus.equals("all")) {
                logger.info("Applying stock status filter for: '{}'", stockStatus);
                final String stockStatusFilter = stockStatus;
                filteredVariants = filteredVariants.stream()
                    .filter(variant -> {
                        if (variant.getStock() == null) return false;
                        switch (stockStatusFilter.toLowerCase()) {
                            case "in-stock":
                                return variant.getStock() > 20;
                            case "low-stock":
                                return variant.getStock() > 0 && variant.getStock() <= 20;
                            case "out-stock":
                                return variant.getStock() == null || variant.getStock() == 0;
                            default:
                                return true;
                        }
                    })
                    .collect(Collectors.toList());
            }
            
            // Apply pagination
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filteredVariants.size());
            
            List<ProductVariant> pageContent = start < filteredVariants.size() ? 
                filteredVariants.subList(start, end) : Collections.emptyList();
            
            productVariantPage = new PageImpl<>(pageContent, pageable, filteredVariants.size());
            
            logger.info("Filter result: {} items found (from {} total after filtering)", 
                       productVariantPage.getTotalElements(), filteredVariants.size());
            
        } catch (Exception e) {
            logger.error("Error in filterInventory: {}", e.getMessage(), e);
            // Fallback to all products with JOIN FETCH
            productVariantPage = inventoryService.getAllProductVariantsWithProductAndCategory(pageable);
        }
        
        InventoryService.InventoryStats stats = inventoryService.getInventoryStats();
        
        // Get all categories for dropdown
        List<String> categories = inventoryService.getAllCategories();
        
        model.addAttribute("title", "Inventory Management");
        model.addAttribute("productVariantPage", productVariantPage);
        model.addAttribute("productVariants", productVariantPage.getContent());
        model.addAttribute("stats", stats);
        model.addAttribute("categories", categories);
        model.addAttribute("search", search);
        model.addAttribute("category", category);
        model.addAttribute("stockStatus", stockStatus);
        
        // Only add pagination attributes if there are results
        if (productVariantPage.getTotalElements() > 0) {
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", productVariantPage.getTotalPages());
            model.addAttribute("totalItems", productVariantPage.getTotalElements());
            model.addAttribute("pageSize", size);
        } else {
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalItems", 0);
            model.addAttribute("pageSize", size);
        }
        
        logger.info("Returning admin inventory view with {} items, total pages: {}", 
                   productVariantPage.getContent().size(), productVariantPage.getTotalPages());
        
        return "admin/admininventory";
    }

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
