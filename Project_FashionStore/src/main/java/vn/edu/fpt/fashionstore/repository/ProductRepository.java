package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.fashionstore.entity.Product;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Query("SELECT DISTINCT p.categoryId FROM Product p WHERE p.categoryId IS NOT NULL")
    List<Long> findAllCategoryIds();

    Product findByProductId(Long productId);
}
