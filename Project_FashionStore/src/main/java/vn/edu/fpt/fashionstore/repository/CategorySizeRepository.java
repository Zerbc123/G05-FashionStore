package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.fashionstore.entity.CategorySize;

public interface CategorySizeRepository extends JpaRepository<CategorySize, Integer> {
}
