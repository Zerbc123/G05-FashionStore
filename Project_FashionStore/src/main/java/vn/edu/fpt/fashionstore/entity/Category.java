package vn.edu.fpt.fashionstore.entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "category_id")
    private int categoryId;

    @Column (name = "category_name")
    private String categoryName;

    @OneToMany(mappedBy = "category")
    private List<CategorySize> sizes;

    public Category() {
    }

    public Category(int categoryId, String categoryName) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public List<CategorySize> getSizes() {
        return sizes;
    }

    public void setSizes(List<CategorySize> sizes) {
        this.sizes = sizes;
    }
}
