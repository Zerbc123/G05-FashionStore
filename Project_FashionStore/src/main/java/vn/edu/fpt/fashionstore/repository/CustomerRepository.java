package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.fashionstore.entity.Customer;

@Repository
public interface CustomerRepository  extends JpaRepository<Customer,Integer> {
    
    Customer findByEmail(String email);
}
