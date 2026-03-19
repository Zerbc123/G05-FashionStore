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
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.fashionstore.repository.OrderRepository;
import vn.edu.fpt.fashionstore.service.ProductService;
import vn.edu.fpt.fashionstore.entity.Product;
import vn.edu.fpt.fashionstore.entity.Order;
import vn.edu.fpt.fashionstore.entity.Account;
import vn.edu.fpt.fashionstore.entity.OrderStatus;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.entity.ProductVariant;
import vn.edu.fpt.fashionstore.service.AccountService;
import vn.edu.fpt.fashionstore.service.InventoryService;

import java.util.Collections;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageImpl;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ProductService productService;
    private final OrderRepository orderRepository;
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
    public String dashboard(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        
        // Lấy dữ liệu từ database
        List<Product> products = productService.getAllProductsWithVariants();
        List<Order> orders = orderRepository.findAll();
        
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
        List<Order> recentOrders = orders.stream()
            .sorted((o1, o2) -> o2.getOrderDate().compareTo(o1.getOrderDate()))
            .limit(5)
            .collect(java.util.stream.Collectors.toList());
        
        // Tính doanh thu theo tháng (Revenue Overview)
        Map<Integer, Double> revenueByMonth = orders.stream()
            .filter(order -> OrderStatus.COMPLETED.equals(order.getStatus()))
            .collect(java.util.stream.Collectors.groupingBy(
                order -> {
                    // Convert Date to LocalDate safely using Calendar
                    java.util.Calendar calendar = java.util.Calendar.getInstance();
                    calendar.setTime(order.getOrderDate());
                    return calendar.get(java.util.Calendar.MONTH) + 1; // Calendar.MONTH is 0-based
                },
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
    public String products(HttpSession session, Model model,
                       @RequestParam(required = false) String search,
                       @RequestParam(required = false) String category) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        // Lấy danh sách sản phẩm từ database
        List<Product> products = productService.getAllProductsWithVariants();

        // Tính toán thống kê
        long totalProducts = products.size();
        long inStockCount = 0;
        long lowStockCount = 0;
        long outOfStockCount = 0;
        
        for (Product product : products) {
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

    // ======== ADMIN PROFILE ========

    @GetMapping("/profile")
    public String adminProfile(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/login";

        String email = (String) session.getAttribute("user");
        if (email == null) return "redirect:/login";

        Account admin = accountService.getAccountByEmail(email);

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
        Page<ProductVariant> productVariantPage;
        InventoryService.InventoryStats stats;
        
        try {
            productVariantPage = inventoryService.getAllProductVariants(pageable);
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
            // Get all variants first
            List<ProductVariant> allVariants = inventoryService.getAllProductVariants();
            
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
                                return variant.getStock() == 0;
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
            // Fallback to all products
            productVariantPage = inventoryService.getAllProductVariants(pageable);
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
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productVariantPage.getTotalPages());
        model.addAttribute("totalItems", productVariantPage.getTotalElements());
        model.addAttribute("pageSize", size);
        
        logger.info("Returning admin inventory view with {} items, total pages: {}", 
                   productVariantPage.getContent().size(), productVariantPage.getTotalPages());
        
        return "admin/admininventory";
    }

}
