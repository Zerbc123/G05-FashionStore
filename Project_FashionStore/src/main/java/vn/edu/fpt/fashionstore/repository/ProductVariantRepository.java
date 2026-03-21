package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.edu.fpt.fashionstore.entity.ProductVariant;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
    
    @Query("SELECT pv FROM ProductVariant pv " +
           "LEFT JOIN FETCH pv.product p " +
           "LEFT JOIN FETCH p.category c")
    List<ProductVariant> findAllWithProductAndCategory();
}
