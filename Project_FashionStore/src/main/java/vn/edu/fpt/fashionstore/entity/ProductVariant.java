package vn.edu.fpt.fashionstore.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "ProductVariant", schema = "dbo")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id")
    private int variantId;

    // ===== FK tới Product =====
    @NotNull(message = "Product is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    // ===== FK tới Color =====
    @NotNull(message = "Color is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id")
    private Color color;

    // ===== FK tới CategorySize =====
    @NotNull(message = "Size is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_size_id")
    private CategorySize categorySize;



    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be greater than or equal to 0")
    @Column(name = "price")
    private Double price;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock must be greater than or equal to 0")
    @Column(name = "stock")
    private Integer stock;

    @Column(name = "image_url", columnDefinition = "NVARCHAR(MAX)")
    private String imageUrl;

    // ===== Constructor =====
    public ProductVariant() {}

    public int getVariantId() {
        return variantId;
    }

    public void setVariantId(int variantId) {
        this.variantId = variantId;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public CategorySize getCategorySize() {
        return categorySize;
    }

    public void setCategorySize(CategorySize categorySize) {
        this.categorySize = categorySize;
    }



    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public ProductVariant(int variantId, Color color, CategorySize categorySize, Double price, Integer stock, String imageUrl, Product product) {
        this.variantId = variantId;
        this.color = color;
        this.categorySize = categorySize;
        this.price = price;
        this.stock = stock;
        this.imageUrl = imageUrl;
        this.product = product;
    }
}
