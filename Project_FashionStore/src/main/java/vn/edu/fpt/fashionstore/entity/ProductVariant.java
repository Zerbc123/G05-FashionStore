package vn.edu.fpt.fashionstore.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ProductVariant")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id")
    private int variantId;

    // ===== FK tới Product =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    // ===== FK tới Color =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id")
    private Color color;

    // ===== FK tới Category_Size =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_size_id")
    private CategorySize categorySize;

    @Column(name = "price")
    private Double price;

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

    public ProductVariant(int variantId, Color color, Double price, Integer stock, String imageUrl, Product product, CategorySize categorySize) {
        this.variantId = variantId;
        this.color = color;
        this.price = price;
        this.stock = stock;
        this.imageUrl = imageUrl;
        this.product = product;
        this.categorySize = categorySize;
    }
}
