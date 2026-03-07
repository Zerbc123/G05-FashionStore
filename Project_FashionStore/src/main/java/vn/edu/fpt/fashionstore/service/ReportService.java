package vn.edu.fpt.fashionstore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.edu.fpt.fashionstore.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class ReportService {

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ProductReportRepository productReportRepository;

    @Autowired
    private CustomerReportRepository customerReportRepository;

    // Revenue Report Methods
    public Map<String, Object> getRevenueReport(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> report = new HashMap<>();
        
        // Get daily revenue from database
        List<Object[]> dailyRevenueData = ordersRepository.getDailyRevenue(startDate, endDate);
        List<Map<String, Object>> dailyRevenue = new ArrayList<>();
        
        for (Object[] row : dailyRevenueData) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", row[0].toString()); // LocalDate to String
            item.put("revenue", (BigDecimal) row[1]);
            // If we have orders count in the query result, use it, otherwise calculate it
            if (row.length > 2) {
                item.put("orders", ((Number) row[2]).longValue());
            } else {
                // Simplified calculation - assume each revenue entry represents at least 1 order
                item.put("orders", 1);
            }
            dailyRevenue.add(item);
        }
        
        // Get monthly revenue from database
        List<Object[]> monthlyRevenueData = ordersRepository.getMonthlyRevenue(startDate, endDate);
        List<Map<String, Object>> monthlyRevenue = new ArrayList<>();
        
        for (Object[] row : monthlyRevenueData) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", row[0].toString()); // Month string
            item.put("revenue", (BigDecimal) row[1]);
            monthlyRevenue.add(item);
        }
        
        // Calculate totals
        BigDecimal totalRevenue = ordersRepository.getTotalRevenue(startDate, endDate);
        Long totalOrders = ordersRepository.getTotalOrders(startDate, endDate);
        BigDecimal averageOrderValue = totalOrders > 0 ? 
            totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, BigDecimal.ROUND_HALF_UP) : 
            BigDecimal.ZERO;
        
        report.put("dailyRevenue", dailyRevenue);
        report.put("monthlyRevenue", monthlyRevenue);
        report.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        report.put("totalOrders", totalOrders != null ? totalOrders : 0L);
        report.put("averageOrderValue", averageOrderValue);
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        
        return report;
    }

    // Best Seller Report Methods
    public Map<String, Object> getBestSellerReport(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> report = new HashMap<>();
        
        // Get best selling products
        List<Object[]> bestSellingProducts = orderItemRepository.getBestSellingProducts(startDate, endDate);
        List<Map<String, Object>> products = new ArrayList<>();
        
        for (Object[] row : bestSellingProducts) {
            Map<String, Object> item = new HashMap<>();
            item.put("productName", row[0]);
            item.put("totalSold", row[1]);
            item.put("revenue", (BigDecimal) row[2]);
            item.put("categoryName", row[3]); // Add category name
            products.add(item);
            
            // Limit to top 5 products
            if (products.size() >= 5) {
                break;
            }
        }
        
        // Get best selling categories
        List<Object[]> bestSellingCategories = orderItemRepository.getBestSellingCategories(startDate, endDate);
        List<Map<String, Object>> categories = new ArrayList<>();
        
        for (Object[] row : bestSellingCategories) {
            Map<String, Object> item = new HashMap<>();
            item.put("categoryName", row[0]);
            item.put("totalSold", row[1]);
            item.put("revenue", (BigDecimal) row[2]);
            categories.add(item);
        }
        
        report.put("products", products);
        report.put("categories", categories);
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        
        return report;
    }

    // Inventory Report Methods
    public Map<String, Object> getInventoryReport() {
        Map<String, Object> report = new HashMap<>();
        
        // Get complete inventory
        List<Object[]> inventoryData = reportRepository.getInventoryReport();
        List<Map<String, Object>> inventory = new ArrayList<>();
        
        int totalProducts = 0;
        int lowStockCount = 0;
        int outOfStockCount = 0;
        
        for (Object[] row : inventoryData) {
            Map<String, Object> item = new HashMap<>();
            item.put("productName", row[0]);
            item.put("stock", row[1]);
            item.put("categoryName", row[2]);
            item.put("soldQuantity", row[3]);
            
            Integer stock = (Integer) row[1];
            if (stock == 0) {
                outOfStockCount++;
            } else if (stock < 10) {
                lowStockCount++;
            }
            
            totalProducts++;
            inventory.add(item);
        }
        
        // Get low stock products
        List<Object[]> lowStockData = reportRepository.getLowStockProducts(10);
        List<Map<String, Object>> lowStockProducts = new ArrayList<>();
        
        for (Object[] row : lowStockData) {
            Map<String, Object> item = new HashMap<>();
            item.put("productName", row[0]);
            item.put("stock", row[1]);
            item.put("categoryName", row[2]);
            item.put("soldQuantity", row[3]);
            lowStockProducts.add(item);
        }
        
        // Get inventory by category
        List<Object[]> categoryData = reportRepository.getInventoryByCategory();
        List<Map<String, Object>> categories = new ArrayList<>();
        
        for (Object[] row : categoryData) {
            Map<String, Object> item = new HashMap<>();
            item.put("categoryName", row[0]);
            item.put("productCount", row[1]);
            item.put("totalStock", row[2]);
            item.put("soldQuantity", row[3]);
            categories.add(item);
        }
        
        report.put("inventory", inventory);
        report.put("lowStockProducts", lowStockProducts);
        report.put("categories", categories);
        report.put("totalProducts", totalProducts);
        report.put("lowStockCount", lowStockCount);
        report.put("outOfStockCount", outOfStockCount);
        report.put("categoryCount", categories.size());
        
        return report;
    }

    // Product Report Methods
    public Map<String, Object> getProductReport() {
        Map<String, Object> report = new HashMap<>();

        // Product summary
        Object[] summary = productReportRepository.getProductSummary();
        report.put("totalProducts", summary[0]);
        report.put("activeProducts", summary[1]);
        report.put("outOfStockProducts", summary[2]);
        report.put("lowStockProducts", summary[3]);

        // Products by category
        List<Object[]> categoryData = productReportRepository.getProductsByCategory();
        List<Map<String, Object>> productsByCategory = new ArrayList<>();
        for (Object[] row : categoryData) {
            Map<String, Object> item = new HashMap<>();
            item.put("categoryName", row[0]);
            item.put("productCount", row[1]);
            item.put("totalStock", row[2]);
            item.put("activeCount", row[3]);
            productsByCategory.add(item);
        }

        // Top selling products
        List<Object[]> topSellingData = productReportRepository.getTopSellingProducts();
        List<Map<String, Object>> topSellingProducts = new ArrayList<>();
        for (Object[] row : topSellingData) {
            Map<String, Object> item = new HashMap<>();
            item.put("productName", row[0]);
            item.put("categoryName", row[1]);
            item.put("price", ((Number) row[2]).doubleValue());
            item.put("stock", row[3]);
            item.put("soldQuantity", row[4]);
            item.put("revenue", ((Number) row[5]).doubleValue());
            topSellingProducts.add(item);
        }

        // All products with sales
        List<Object[]> allProductsData = productReportRepository.getAllProductsWithSales();
        List<Map<String, Object>> allProducts = new ArrayList<>();
        for (Object[] row : allProductsData) {
            Map<String, Object> item = new HashMap<>();
            item.put("productName", row[0]);
            item.put("categoryName", row[1]);
            item.put("price", ((Number) row[2]).doubleValue());
            item.put("stock", row[3]);
            item.put("soldQuantity", row[4]);
            item.put("revenue", ((Number) row[5]).doubleValue());
            allProducts.add(item);
        }

        report.put("productsByCategory", productsByCategory);
        report.put("topSellingProducts", topSellingProducts);
        report.put("allProducts", allProducts);

        return report;
    }

    // Customer Report Methods
    public Map<String, Object> getCustomerReport(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> report = new HashMap<>();

        // Customer summary
        Object[] summary = customerReportRepository.getCustomerSummary(startDate);
        report.put("totalCustomers", summary[0]);
        report.put("activeCustomers", summary[1]);
        report.put("newCustomers", summary[2]);
        report.put("inactiveCustomers", summary[3]);

        // Top customers
        List<Object[]> topCustomersData = customerReportRepository.getTopCustomers();
        List<Map<String, Object>> topCustomers = new ArrayList<>();
        for (Object[] row : topCustomersData) {
            Map<String, Object> item = new HashMap<>();
            item.put("customerName", row[0]);
            item.put("email", row[1]);
            item.put("phone", row[2]);
            item.put("totalOrders", row[3]);
            item.put("totalSpent", ((Number) row[4]).doubleValue());
            item.put("averageOrderValue", ((Number) row[5]).doubleValue());
            item.put("joinDate", row[6]);
            topCustomers.add(item);
        }

        // Registration summary
        List<Object[]> registrationData = customerReportRepository.getRegistrationSummary(startDate);
        List<Map<String, Object>> registrationSummary = new ArrayList<>();
        for (Object[] row : registrationData) {
            Map<String, Object> item = new HashMap<>();
            item.put("period", row[0]);
            item.put("newCustomers", row[1]);
            item.put("activeCustomers", row[2]);
            item.put("registrationRate", ((Number) row[3]).doubleValue());
            registrationSummary.add(item);
        }

        // All customers
        List<Object[]> allCustomersData = customerReportRepository.getAllCustomersWithOrders();
        List<Map<String, Object>> allCustomers = new ArrayList<>();
        for (Object[] row : allCustomersData) {
            Map<String, Object> item = new HashMap<>();
            item.put("customerName", row[0]);
            item.put("email", row[1]);
            item.put("phone", row[2]);
            item.put("totalOrders", row[3]);
            item.put("totalSpent", ((Number) row[4]).doubleValue());
            item.put("status", row[5]);
            item.put("joinDate", row[6]);
            allCustomers.add(item);
        }

        report.put("topCustomers", topCustomers);
        report.put("registrationSummary", registrationSummary);
        report.put("allCustomers", allCustomers);
        report.put("startDate", startDate);
        report.put("endDate", endDate);

        return report;
    }
}
