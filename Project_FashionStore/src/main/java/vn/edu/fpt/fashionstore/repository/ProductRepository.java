package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.fashionstore.entity.Category;
import vn.edu.fpt.fashionstore.entity.Product;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    // LẤY CATEGORY ID TỪ PRODUCT
    @Query("SELECT DISTINCT p.category FROM Product p")
    List<Category> findAllCategories();

    Product findByProductId(Long productId);
}
