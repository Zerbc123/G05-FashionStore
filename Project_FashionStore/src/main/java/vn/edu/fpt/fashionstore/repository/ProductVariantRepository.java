package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.fashionstore.entity.ProductVariant;
import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
    List<ProductVariant> findByProduct_ProductId(Long productId);
}
