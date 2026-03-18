package vn.edu.fpt.fashionstore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.fashionstore.entity.Category;
import vn.edu.fpt.fashionstore.repository.CategoriesRepository;
import vn.edu.fpt.fashionstore.repository.ProductRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoriesRepository categoryRepository;
    private final ProductRepository productRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getByName(String categoryName) {
        return categoryRepository.findByCategoryName(categoryName);
    }

    public Category getById(int id) {
        return categoryRepository.findById(id).orElse(null);
    }

    public void save(Category category) {
        categoryRepository.save(category);
    }

    @Transactional
    public boolean delete(int categoryId) {

        boolean isUsed = productRepository.existsByCategory_CategoryId(categoryId);

        if (isUsed) {
            return false;
        }

        categoryRepository.deleteById(categoryId);
        return true;
    }
}