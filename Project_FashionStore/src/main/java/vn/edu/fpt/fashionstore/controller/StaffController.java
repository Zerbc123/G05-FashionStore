package vn.edu.fpt.fashionstore.controller;

import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.fashionstore.entity.ProductVariant;
import vn.edu.fpt.fashionstore.repository.AccountRepository;
import vn.edu.fpt.fashionstore.service.AccountService;
import vn.edu.fpt.fashionstore.service.InventoryService;
import vn.edu.fpt.fashionstore.util.RoleUtils;

import java.util.List;

@Controller
@RequestMapping("/staff")
public class StaffController {

    private static final Logger logger = LoggerFactory.getLogger(StaffController.class);

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private InventoryService inventoryService;

    private boolean isStaff(HttpSession session) {
        String role = (String) session.getAttribute("userRole");

        if (role == null) {
            return false;
        }

        return "Admin".equalsIgnoreCase(role)
                || role.contains("Nhân viên bán hàng (Sale)")
                || role.contains("Quản lý kho (Stock)")
                || role.contains("Hỗ trợ khách hàng (Support)")
                || role.contains("Quản lý cửa hàng (Manager)");
    }

    private boolean isSupport(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        return role != null && role.contains("Hỗ trợ khách hàng (Support)");
    }

    private boolean isSale(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        return role != null && role.contains("Nhân viên bán hàng (Sale)");
    }

    private boolean isStock(HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        return role != null && role.contains("Quản lý kho (Stock)");
    }

    // ======== STAFF VIEW ========

    @GetMapping({"", "/", "/view"})
    public String staffView(HttpSession session, Model model) {
        if (!isStaff(session)) return "redirect:/login";
        model.addAttribute("title", "Staff Panel");
        return "staff/staffview";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!RoleUtils.canAccessDashboard(session)) {
            model.addAttribute("error", RoleUtils.getAccessDeniedMessage("dashboard"));
            return "staff/access_denied";
        }
        
        // Add permission attributes for UI control
        boolean canManage = RoleUtils.canManageDashboard(session);
        model.addAttribute("canManageDashboard", canManage);
        
        model.addAttribute("title", "Staff Dashboard");
        return "staff/staffdashboard";
    }

    // Quản lý đơn hàng (Confirm Orders)
    @GetMapping("/orders")
    public String orders(HttpSession session, Model model) {
        if (!RoleUtils.canViewOrders(session)) {
            return "redirect:/login";
        }

        // Check if user can manage orders (Admin, Sale, Manager)
        boolean canManage = RoleUtils.canManageOrders(session);
        model.addAttribute("canManageOrders", canManage);

        model.addAttribute("title", "Order Management");
        return "admin/adminorder";
    }

    @GetMapping("/inventory")
    public String inventory(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             HttpSession session, Model model) {
        if (!RoleUtils.canViewInventory(session)) {
            return "redirect:/login";
        }
        
        // Check if user can manage inventory (Admin, Stock)
        boolean canManage = RoleUtils.canManageInventory(session);
        model.addAttribute("canManageInventory", canManage);
        
        // Fetch paginated data from database
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductVariant> productVariantPage = inventoryService.getAllProductVariantsWithProductAndCategory(pageable);
        InventoryService.InventoryStats stats = inventoryService.getInventoryStats();
        
        // Get all categories for dropdown
        List<String> categories = inventoryService.getAllCategories();
        
        model.addAttribute("title", "Inventory Management");
        model.addAttribute("productVariantPage", productVariantPage);
        model.addAttribute("productVariants", productVariantPage.getContent());
        model.addAttribute("stats", stats);
        model.addAttribute("categories", categories);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productVariantPage.getTotalPages());
        model.addAttribute("totalItems", productVariantPage.getTotalElements());
        model.addAttribute("pageSize", size);
        
        return "staff/inventory";
    }

    @PostMapping("/inventory/update-stock")
    public String updateStock(@RequestParam Integer variantId, 
                            @RequestParam Integer newStock,
                            HttpSession session, 
                            RedirectAttributes redirectAttributes) {
        if (!RoleUtils.canManageInventory(session)) {
            redirectAttributes.addFlashAttribute("error", RoleUtils.getAccessDeniedMessage("inventory"));
            return "redirect:/staff/inventory";
        }
        
        try {
            inventoryService.updateStock(variantId, newStock);
            redirectAttributes.addFlashAttribute("success", "Cập nhật tồn kho thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi cập nhật tồn kho: " + e.getMessage());
        }
        
        return "redirect:/staff/inventory";
    }

    @GetMapping("/inventory/filter")
    public String filterInventory(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 @RequestParam(required = false) String search,
                                 @RequestParam(required = false) String category,
                                 @RequestParam(required = false) String stockStatus,
                                 HttpSession session, Model model) {
        logger.info("Filter inventory called with params: page={}, size={}, search='{}', category='{}', stockStatus='{}'", 
                   page, size, search, category, stockStatus);
        
        if (!isStaff(session)) {
            logger.warn("User not authenticated as staff, redirecting to login");
            return "redirect:/login";
        }
        
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
                    .collect(java.util.stream.Collectors.toList());
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
                    .collect(java.util.stream.Collectors.toList());
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
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // Apply pagination to filtered results
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filteredVariants.size());
            List<ProductVariant> pageContent = start < filteredVariants.size() ? 
                filteredVariants.subList(start, end) : java.util.Collections.emptyList();
            
            productVariantPage = new org.springframework.data.domain.PageImpl<>(pageContent, pageable, filteredVariants.size());
            
        } catch (Exception e) {
            logger.error("Error filtering inventory: {}", e.getMessage(), e);
            // Fallback to all products if filtering fails
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
        
        logger.info("Returning inventory view with {} items, total pages: {}", 
                   productVariantPage.getContent().size(), productVariantPage.getTotalPages());
        
        return "staff/inventory";
    }

    // change-password endpoints removed for staff; only customers can change password now.

    // ======== STAFF PROFILE ========

    @GetMapping("/profile")
    public String staffProfile(HttpSession session, Model model) {
        if (!isStaff(session)) return "redirect:/login";

        String email = (String) session.getAttribute("user");
        if (email == null) return "redirect:/login";

        var staff = accountService.getAccountByEmail(email);

        if (staff == null) {
            model.addAttribute("error", "Không tìm thấy thông tin nhân viên!");
            return "staff/staff_profile";
        }

        model.addAttribute("title", "Staff Profile");
        model.addAttribute("staff", staff);

        return "staff/staff_profile";
    }

}