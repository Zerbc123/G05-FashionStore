package vn.edu.fpt.fashionstore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.entity.Order;
import vn.edu.fpt.fashionstore.repository.AccountRepository;
import vn.edu.fpt.fashionstore.repository.CustomerRepository;
import vn.edu.fpt.fashionstore.repository.OrderRepository;

import java.util.*;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;

    // Get all customers with pagination
    public Page<Customer> getAllCustomers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return customerRepository.findAll(pageable);
    }

    // Get all customers (without pagination for statistics)
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    // Search customers by name or email
    public Page<Customer> searchCustomers(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        final String searchKeyword = keyword;
        
        if (searchKeyword == null || searchKeyword.trim().isEmpty()) {
            return customerRepository.findAll(pageable);
        }
        
        final String finalKeyword = searchKeyword.toLowerCase().trim();
        List<Customer> allCustomers = customerRepository.findAll();
        
        List<Customer> filteredCustomers = allCustomers.stream()
            .filter(customer -> 
                (customer.getFullName() != null && customer.getFullName().toLowerCase().contains(finalKeyword)) ||
                (customer.getEmail() != null && customer.getEmail().toLowerCase().contains(finalKeyword)) ||
                (customer.getPhone() != null && customer.getPhone().contains(finalKeyword))
            )
            .collect(Collectors.toList());
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredCustomers.size());
        List<Customer> pageContent = start >= filteredCustomers.size() ? 
            Collections.emptyList() : filteredCustomers.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, filteredCustomers.size());
    }

    // Get customer by ID
    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }

    // Get customer by email
    public Customer getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email);
    }

    // Save or update customer
    public Customer saveCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    // Update customer account status
    public boolean updateCustomerAccountStatus(Long customerId, String status) {
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        if (customerOpt.isPresent() && customerOpt.get().getAccount() != null) {
            Customer customer = customerOpt.get();
            customer.getAccount().setStatus(status);
            // Save the Account entity to ensure status changes are persisted
            accountRepository.save(customer.getAccount());
            customerRepository.save(customer);
            return true;
        }
        return false;
    }

    // Delete customer by ID
    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
    }

    // Get customer statistics
    public Map<String, Object> getCustomerStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        List<Customer> allCustomers = customerRepository.findAll();
        List<Order> allOrders = orderRepository.findAll();
        
        // Total customers
        stats.put("totalCustomers", allCustomers.size());
        
        // Active customers (customers with active accounts)
        long activeCustomers = allCustomers.stream()
            .filter(customer -> 
                customer.getAccount() != null && 
                customer.getAccount().getStatus() != null && 
                "active".equalsIgnoreCase(customer.getAccount().getStatus())
            )
            .count();
        
        stats.put("activeCustomers", activeCustomers);
        
        // VIP customers - chỉ tính những customer có account active và đạt điều kiện VIP
        Map<Long, Double> customerSpending = new HashMap<>();
        Map<Long, Long> customerOrderCount = new HashMap<>();
        
        allOrders.stream()
            .filter(order -> order.getCustomer() != null && "COMPLETED".equals(order.getStatus()))
            .forEach(order -> {
                Long customerId = order.getCustomer().getCustomerId();
                Double amount = order.getTotalAmount() != null ? order.getTotalAmount() : 0.0;
                customerSpending.merge(customerId, amount, Double::sum);
                customerOrderCount.merge(customerId, 1L, Long::sum);
            });
        
        long vipCount = 0;
        for (Customer customer : allCustomers) {
            // Chỉ xét các customer có account và account đang active
            if (customer.getAccount() != null && 
                customer.getAccount().getStatus() != null && 
                "active".equalsIgnoreCase(customer.getAccount().getStatus())) {
                
                double totalSpent = customerSpending.getOrDefault(customer.getCustomerId(), 0.0);
                long totalOrders = customerOrderCount.getOrDefault(customer.getCustomerId(), 0L);
                
                // Kiểm tra điều kiện VIP
                if (totalSpent > 1000.0 || totalOrders > 10) {
                    vipCount++;
                }
            }
        }
        
        stats.put("vipCustomers", vipCount);
        
        // New customers this month
        LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
        long newCustomersThisMonth = allCustomers.stream()
            .filter(customer -> customer.getCreatedDate() != null && customer.getCreatedDate().isAfter(oneMonthAgo))
            .count();
        
        stats.put("newCustomersThisMonth", newCustomersThisMonth);
        
        return stats;
    }

    // Get customer details with order information
    public Map<String, Object> getCustomerDetails(Long customerId) {
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        if (!customerOpt.isPresent()) {
            return Collections.emptyMap();
        }
        
        Customer customer = customerOpt.get();
        Map<String, Object> details = new HashMap<>();
        
        // Customer info
        details.put("customer", customer);
        
        // Customer's orders
        List<Order> customerOrders = orderRepository.findByCustomer_CustomerIdOrderByOrderDateDesc(customerId);
        details.put("orders", customerOrders);
        
        // Order statistics
        long totalOrders = customerOrders.size();
        long completedOrders = customerOrders.stream()
            .filter(order -> order.getStatus() != null && order.getStatus().name().equals("COMPLETED"))
            .count();
        
        double totalSpent = customerOrders.stream()
            .filter(order -> order.getStatus() != null && order.getStatus().name().equals("COMPLETED"))
            .mapToDouble(order -> order.getTotalAmount() != null ? order.getTotalAmount() : 0.0)
            .sum();
        
        details.put("totalOrders", totalOrders);
        details.put("completedOrders", completedOrders);
        details.put("totalSpent", totalSpent);
        
        // Customer status - Ưu tiên status từ Account, sau đó tính toán dựa trên hoạt động
        String status = "Active"; // Mặc định là Active nếu account tồn tại
        
        // Kiểm tra status từ account trước
        if (customer.getAccount() != null && customer.getAccount().getStatus() != null) {
            String accountStatus = customer.getAccount().getStatus();
            if ("inactive".equalsIgnoreCase(accountStatus) || "blocked".equalsIgnoreCase(accountStatus)) {
                status = "Inactive";
            } else if ("active".equalsIgnoreCase(accountStatus)) {
                status = "Active";
            }
        } else {
            // Nếu không có account, tính dựa trên hoạt động
            if (totalOrders > 0) {
                status = "Active";
            } else {
                status = "Inactive";
            }
        }
        details.put("status", status);
        
        return details;
    }
}
