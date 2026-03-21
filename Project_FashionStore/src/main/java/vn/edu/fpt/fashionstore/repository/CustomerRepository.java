package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.fashionstore.entity.Customer;
import java.util.Optional;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    Customer findByEmail(String email);
    
    List<Customer> findByFullNameContainingIgnoreCase(String fullName);
    
    List<Customer> findByEmailContainingIgnoreCase(String email);
    
    List<Customer> findByPhoneContaining(String phone);
    Optional<Customer> findByAccountEmail(String email);
}
