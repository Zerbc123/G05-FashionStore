package vn.edu.fpt.fashionstore.entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "category_id")
    private int cateId;

    @Column (name = "category_name")
    private String categoryName;

    @OneToMany(mappedBy = "category")
    private List<CategorySize> sizes;

    public Category() {
    }

    public Category(int cateId, String categoryName) {
        this.cateId = cateId;
        this.categoryName = categoryName;
    }

    public int getCateId() {
        return cateId;
    }

    public void setCateId(int cateId) {
        this.cateId = cateId;
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
