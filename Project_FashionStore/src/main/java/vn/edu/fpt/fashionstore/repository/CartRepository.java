package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.fashionstore.entity.CartItem;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.entity.ProductVariant;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<CartItem, Integer> {

    // Tìm tất cả các sản phẩm trong giỏ hàng của một khách hàng
    @Query("SELECT ci FROM CartItem ci " +
           "JOIN FETCH ci.productVariant pv " +
           "JOIN FETCH pv.product " +
           "JOIN FETCH pv.color " +
           "JOIN FETCH pv.categorySize " +
           "WHERE ci.customer = :customer")
    List<CartItem> findByCustomer(@Param("customer") Customer customer);

    // Tìm một sản phẩm cụ thể trong giỏ hàng của khách hàng
    Optional<CartItem> findByCustomerAndProductVariant(Customer customer, ProductVariant productVariant);
}
