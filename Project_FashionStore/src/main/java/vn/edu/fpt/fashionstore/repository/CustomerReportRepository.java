package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.entity.OrderStatus;

import java.time.LocalDate;
import java.util.List;

public interface CustomerReportRepository extends JpaRepository<Customer, Integer> {

       // Simple test query to check if we can get any customers
       @Query("SELECT COUNT(c) FROM Customer c")
       Long getTotalCustomerCount();

       // Simple test query to check if we can get any orders
       @Query("SELECT COUNT(o) FROM Order o")
       Long getTotalOrderCount();

       // Get customer summary
       @Query("SELECT " +
                     "COUNT(DISTINCT c) as totalCustomers, " +
                     "(SELECT COUNT(DISTINCT c2) FROM Customer c2 WHERE EXISTS (" +
                     "  SELECT 1 FROM Order o2 WHERE o2.customer.customerId = c2.customerId AND o2.status IN :statuses"
                     +
                     ")) as activeCustomers, " +
                     "SUM(CASE WHEN c.createdDate >= :startDate THEN 1 ELSE 0 END) as newCustomers, " +
                     "(SELECT COUNT(DISTINCT c3) FROM Customer c3 WHERE NOT EXISTS (" +
                     "  SELECT 1 FROM Order o3 WHERE o3.customer.customerId = c3.customerId AND o3.status IN :statuses"
                     +
                     ")) as inactiveCustomers " +
                     "FROM Customer c")
       List<Object[]> getCustomerSummary(@Param("startDate") LocalDate startDate,
                     @Param("statuses") List<OrderStatus> statuses);

       // Get top customers by revenue
       @Query("SELECT c.fullName, c.email, c.phone, " +
                     "COUNT(o) as totalOrders, " +
                     "COALESCE(SUM(o.totalAmount), 0) as totalSpent, " +
                     "COALESCE(SUM(o.totalAmount) / NULLIF(COUNT(o), 0), 0) as averageOrderValue, " +
                     "c.createdDate " +
                     "FROM Customer c " +
                     "LEFT JOIN Order o ON c.customerId = o.customer.customerId AND o.status IN :statuses " +
                     "GROUP BY c.customerId, c.fullName, c.email, c.phone, c.createdDate " +
                     "ORDER BY totalSpent DESC")
       List<Object[]> getTopCustomers(@Param("statuses") List<OrderStatus> statuses);

       // Get all customers with order info
       @Query("SELECT c.fullName, c.email, c.phone, " +
                     "COUNT(o) as totalOrders, " +
                     "COALESCE(SUM(o.totalAmount), 0) as totalSpent, " +
                     "CASE WHEN COUNT(o) > 0 THEN 'Active' ELSE 'Inactive' END as status, " +
                     "c.createdDate " +
                     "FROM Customer c " +
                     "LEFT JOIN Order o ON c.customerId = o.customer.customerId AND o.status IN :statuses " +
                     "GROUP BY c.customerId, c.fullName, c.email, c.phone, c.createdDate " +
                     "ORDER BY c.fullName")
       List<Object[]> getAllCustomersWithOrders(@Param("statuses") List<OrderStatus> statuses);

       // Get registration summary by period
       @Query("SELECT " +
                     "CONCAT(YEAR(c.createdDate), '-', CASE WHEN MONTH(c.createdDate) < 10 THEN '0' ELSE '' END, MONTH(c.createdDate)) as period, "
                     +
                     "COUNT(c) as newCustomers, " +
                     "SUM(CASE WHEN o IS NOT NULL THEN 1 ELSE 0 END) as activeCustomers, " +
                     "0.0 as registrationRate " +
                     "FROM Customer c " +
                     "LEFT JOIN Order o ON c.customerId = o.customer.customerId AND o.status IN :statuses " +
                     "WHERE c.createdDate >= :startDate " +
                     "GROUP BY YEAR(c.createdDate), MONTH(c.createdDate) " +
                     "ORDER BY YEAR(c.createdDate), MONTH(c.createdDate)")
       List<Object[]> getRegistrationSummary(@Param("startDate") LocalDate startDate,
                     @Param("statuses") List<OrderStatus> statuses);
}