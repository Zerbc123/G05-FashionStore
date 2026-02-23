package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.fashionstore.entity.Category;

public interface CategoriesRepository extends JpaRepository<Category, Integer> {
}
