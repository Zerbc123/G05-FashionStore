package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.entity.Product;
import vn.edu.fpt.fashionstore.entity.Wishlist;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Integer> {
    
    List<Wishlist> findByCustomer(Customer customer);
    
    Optional<Wishlist> findByCustomerAndProduct(Customer customer, Product product);
    
    boolean existsByCustomerAndProduct(Customer customer, Product product);
    
    void deleteByCustomerAndProduct(Customer customer, Product product);
    
    @Query("SELECT COUNT(w) FROM Wishlist w WHERE w.customer = :customer")
    long countByCustomer(@Param("customer") Customer customer);
}
