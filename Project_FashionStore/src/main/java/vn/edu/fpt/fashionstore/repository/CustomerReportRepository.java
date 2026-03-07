package vn.edu.fpt.fashionstore.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.fashionstore.entity.Customer;

import java.time.LocalDate;
import java.util.List;

public interface CustomerReportRepository extends JpaRepository<Customer, Integer> {

    // Get customer summary
    @Query("SELECT COUNT(DISTINCT c) as totalCustomers, " +
           "SUM(CASE WHEN o.orderId IS NOT NULL THEN 1 ELSE 0 END) as activeCustomers, " +
           "SUM(CASE WHEN c.createdDate >= :startDate THEN 1 ELSE 0 END) as newCustomers, " +
           "SUM(CASE WHEN o.orderId IS NULL THEN 1 ELSE 0 END) as inactiveCustomers " +
           "FROM Customer c " +
           "LEFT JOIN c.account a " +
           "LEFT JOIN Orders o ON a.accountId = o.accountId")
    Object[] getCustomerSummary(@Param("startDate") LocalDate startDate);

    // Get top customers by revenue
    @Query("SELECT c.fullName, a.email, a.phone, " +
           "COUNT(o) as totalOrders, " +
           "COALESCE(SUM(o.totalAmount), 0) as totalSpent, " +
           "COALESCE(SUM(o.totalAmount) / COUNT(o), 0) as averageOrderValue, " +
           "c.createdDate " +
           "FROM Customer c " +
           "LEFT JOIN c.account a " +
           "LEFT JOIN Orders o ON a.accountId = o.accountId " +
           "GROUP BY c.customerId " +
           "ORDER BY totalSpent DESC")
    List<Object[]> getTopCustomers();

    // Get all customers with order info
    @Query("SELECT c.fullName, a.email, a.phone, " +
           "COUNT(o) as totalOrders, " +
           "COALESCE(SUM(o.totalAmount), 0) as totalSpent, " +
           "CASE WHEN COUNT(o) > 0 THEN 'Active' ELSE 'Inactive' END as status, " +
           "c.createdDate " +
           "FROM Customer c " +
           "LEFT JOIN c.account a " +
           "LEFT JOIN Orders o ON a.accountId = o.accountId " +
           "GROUP BY c.customerId " +
           "ORDER BY c.fullName")
    List<Object[]> getAllCustomersWithOrders();

    // Get registration summary by period
    @Query("SELECT " +
           "CASE " +
           "  WHEN c.createdDate >= :startDate AND c.createdDate < DATEADD(day, 7, :startDate) THEN 'This Week' " +
           "  WHEN c.createdDate >= DATEADD(day, -30, :startDate) AND c.createdDate < :startDate THEN 'Last Month' " +
           "  WHEN c.createdDate >= DATEADD(day, -90, :startDate) AND c.createdDate < DATEADD(day, -30, :startDate) THEN '2-3 Months Ago' " +
           "  ELSE 'Older' " +
           "END as period, " +
           "COUNT(c) as newCustomers, " +
           "SUM(CASE WHEN o.orderId IS NOT NULL THEN 1 ELSE 0 END) as activeCustomers, " +
           "(COUNT(c) * 100.0 / (SELECT COUNT(*) FROM Customer)) as registrationRate " +
           "FROM Customer c " +
           "LEFT JOIN c.account a " +
           "LEFT JOIN Orders o ON a.accountId = o.accountId " +
           "WHERE c.createdDate >= DATEADD(day, -90, :startDate) " +
           "GROUP BY " +
           "CASE " +
           "  WHEN c.createdDate >= :startDate AND c.createdDate < DATEADD(day, 7, :startDate) THEN 'This Week' " +
           "  WHEN c.createdDate >= DATEADD(day, -30, :startDate) AND c.createdDate < :startDate THEN 'Last Month' " +
           "  WHEN c.createdDate >= DATEADD(day, -90, :startDate) AND c.createdDate < DATEADD(day, -30, :startDate) THEN '2-3 Months Ago' " +
           "  ELSE 'Older' " +
           "END")
    List<Object[]> getRegistrationSummary(@Param("startDate") LocalDate startDate);
}
