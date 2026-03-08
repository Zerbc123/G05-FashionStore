package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.fashionstore.entity.OrderItem;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    // Check if any order item exists for a specific variant
    boolean existsByProductVariantVariantId(int variantId);
    
    // Get all order items for a specific variant
    List<OrderItem> findByProductVariantVariantId(int variantId);
    
    // Count order items for a specific variant
    long countByProductVariantVariantId(int variantId);
}
