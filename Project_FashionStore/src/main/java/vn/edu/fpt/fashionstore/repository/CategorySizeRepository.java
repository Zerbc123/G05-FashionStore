package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.fashionstore.entity.CategorySize;

@Repository
public interface CategorySizeRepository extends JpaRepository<CategorySize, Integer> {
}
