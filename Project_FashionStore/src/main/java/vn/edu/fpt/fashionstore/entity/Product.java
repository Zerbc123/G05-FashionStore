package vn.edu.fpt.fashionstore.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.List;

@Entity
@Table(name = "Product")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 255, message = "Product name must be between 3 and 255 characters")
    @Pattern(regexp = "^[^0-9]*$", message = "Product name cannot contain numbers")
    @Column(name = "product_name", nullable = false)
    private String productName;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @NotNull(message = "Category is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<ProductVariant> variants;
    
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Wishlist> wishlists;

    public List<ProductVariant> getVariants() {
        return variants;
    }

    public void setVariants(List<ProductVariant> variants) {
        this.variants = variants;
    }
    
    public List<Wishlist> getWishlists() {
        return wishlists;
    }
    
    public void setWishlists(List<Wishlist> wishlists) {
        this.wishlists = wishlists;
    }

    public Product() {
    }

    public Product(Long productId, String productName, String description,
            Category category, Long accountId) {
        this.productId = productId;
        this.productName = productName;
        this.description = description;
        this.category = category;
        this.accountId = accountId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public boolean isOutOfStock() {
        if (variants == null || variants.isEmpty()) {
            return true;
        }
        for (ProductVariant variant : variants) {
            if (variant.getStock() != null && variant.getStock() > 0) {
                return false;
            }
        }
        return true;
    }

    public int getTotalStock() {
        if (variants == null || variants.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (ProductVariant variant : variants) {
            if (variant.getStock() != null) {
                total += variant.getStock();
            }
        }
        return total;
    }
}
