package vn.edu.fpt.fashionstore.entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "Category_Size")
public class CategorySize {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_size_id")
    private int categorySizeId;

    @Column(name = "size_name")
    private String sizeName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "categorySize")
    private List<ProductVariant> variants;

    public CategorySize(int categorySizeId, String sizeName) {
        this.categorySizeId = categorySizeId;
        this.sizeName = sizeName;
    }

    public CategorySize() {
    }

    public int getCategorySizeId() {
        return categorySizeId;
    }

    public void setCategorySizeId(int categorySizeId) {
        this.categorySizeId = categorySizeId;
    }

    public String getSizeName() {
        return sizeName;
    }

    public void setSizeName(String sizeName) {
        this.sizeName = sizeName;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public List<ProductVariant> getVariants() {
        return variants;
    }

    public void setVariants(List<ProductVariant> variants) {
        this.variants = variants;
    }
}
