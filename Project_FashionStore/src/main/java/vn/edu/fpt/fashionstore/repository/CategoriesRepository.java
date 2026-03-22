package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.fashionstore.entity.Category;

import java.util.List;

public interface CategoriesRepository extends JpaRepository<Category, Integer> {
    Category findByCategoryName(String categoryName);
    List<Category> findByCategoryNameContainingIgnoreCase(String keyword);
}
