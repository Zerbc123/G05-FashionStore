package vn.edu.fpt.fashionstore.util;

import jakarta.servlet.http.HttpSession;

/**
 * Utility class for role-based access control
 */
public class RoleUtils {
    
    // Role constants
    public static final String ROLE_ADMIN = "Admin";
    public static final String ROLE_CUSTOMER = "Customer";
    public static final String ROLE_SALE = "Nhân viên bán hàng (Sale)";
    public static final String ROLE_STOCK = "Quản lý kho (Stock)";
    public static final String ROLE_SUPPORT = "Hỗ trợ khách hàng (Support)";
    public static final String ROLE_MANAGER = "Quản lý cửa hàng (Manager)";
    
    /**
     * Get current user role from session
     */
    public static String getCurrentRole(HttpSession session) {
        return (String) session.getAttribute("userRole");
    }
    
    /**
     * Check if current user has specific role
     */
    public static boolean hasRole(HttpSession session, String requiredRole) {
        String currentRole = getCurrentRole(session);
        return requiredRole.equals(currentRole);
    }
    
    /**
     * Check if current user is Admin
     */
    public static boolean isAdmin(HttpSession session) {
        return hasRole(session, ROLE_ADMIN);
    }
    
    /**
     * Check if current user is Support
     */
    public static boolean isSupport(HttpSession session) {
        return hasRole(session, ROLE_SUPPORT);
    }
    
    /**
     * Check if current user is Sale
     */
    public static boolean isSale(HttpSession session) {
        return hasRole(session, ROLE_SALE);
    }
    
    /**
     * Check if current user is Stock
     */
    public static boolean isStock(HttpSession session) {
        return hasRole(session, ROLE_STOCK);
    }
    
    /**
     * Check if current user is Manager
     */
    public static boolean isManager(HttpSession session) {
        return hasRole(session, ROLE_MANAGER);
    }
    
    /**
     * Check if current user can access inventory management
     */
    public static boolean canManageInventory(HttpSession session) {
        return isAdmin(session) || isStock(session);
    }
    
    /**
     * Check if current user can view inventory (read-only)
     */
    public static boolean canViewInventory(HttpSession session) {
        return isAdmin(session) || isStock(session) || isSupport(session) || 
               isSale(session) || isManager(session);
    }
    
    /**
     * Check if current user can manage orders
     */
    public static boolean canManageOrders(HttpSession session) {
        return isAdmin(session) || isSale(session) || isManager(session);
    }
    
    /**
     * Check if current user can view orders (read-only)
     */
    public static boolean canViewOrders(HttpSession session) {
        return isAdmin(session) || isSale(session) || isSupport(session) || 
               isStock(session) || isManager(session);
    }
    
    /**
     * Check if current user can manage support
     */
    public static boolean canManageSupport(HttpSession session) {
        return isAdmin(session) || isSupport(session) || isManager(session);
    }
    
    /**
     * Check if current user can access dashboard (Admin, Manager, Support)
     */
    public static boolean canAccessDashboard(HttpSession session) {
        return isAdmin(session) || isManager(session) || isSupport(session);
    }
    
    /**
     * Check if current user can manage dashboard (Admin, Manager only)
     */
    public static boolean canManageDashboard(HttpSession session) {
        return isAdmin(session) || isManager(session);
    }
    
    /**
     * Get access denied message for specific action
     */
    public static String getAccessDeniedMessage(String action) {
        switch (action) {
            case "inventory":
                return "Bạn không có quyền quản lý kho. Chỉ Admin và Quản lý kho mới có thể thực hiện thao tác này.";
            case "orders":
                return "Bạn không có quyền quản lý đơn hàng. Chỉ Admin và Nhân viên bán hàng mới có thể thực hiện thao tác này.";
            case "support":
                return "Bạn không có quyền quản lý hỗ trợ khách hàng. Chỉ Admin và Hỗ trợ khách hàng mới có thể thực hiện thao tác này.";
            case "dashboard":
                return "Bạn không có quyền truy cập Dashboard. Chỉ Admin và Quản lý cửa hàng mới có thể truy cập.";
            default:
                return "Bạn không có quyền thực hiện thao tác này.";
        }
    }
}
