package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.fashionstore.entity.ProductVariant;
import java.util.List;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
    List<ProductVariant> findByProduct_ProductId(Long productId);
    void deleteByProduct_ProductId(Long productId);
    
    // Count methods for statistics
    long countByStockGreaterThan(int stock);
    long countByStockBetween(int minStock, int maxStock);
    long countByStockEquals(int stock);
    
    // Search method for variants
    @Query("SELECT pv FROM ProductVariant pv WHERE " +
           "LOWER(pv.product.productName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(pv.color.colorName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(pv.categorySize.sizeName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "CAST(pv.price AS string) LIKE CONCAT('%', :searchTerm, '%')")
    Page<ProductVariant> searchVariants(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    @Query("SELECT pv FROM ProductVariant pv " +
           "LEFT JOIN FETCH pv.product p " +
           "LEFT JOIN FETCH p.category c")
    List<ProductVariant> findAllWithProductAndCategory();
}
