package vn.edu.fpt.fashionstore.entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class Color {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column (name = "color_id")
    private int colorId;

    @Column(name = "color_name")
    private String colorName;

    @Column(name = "color_code")
    private String colorCode;

    @OneToMany (mappedBy = "color")
    private List<ProductVariant> productVariants;

    public Color() {
    }

    public Color(int colorId, String colorName) {
        this.colorId = colorId;
        this.colorName = colorName;
    }

    public Color(int colorId, String colorName, String colorCode) {
        this.colorId = colorId;
        this.colorName = colorName;
        this.colorCode = colorCode;
    }

    public int getColorId() {
        return colorId;
    }

    public void setColorId(int colorId) {
        this.colorId = colorId;
    }

    public String getColorName() {
        return colorName;
    }

    public void setColorName(String colorName) {
        this.colorName = colorName;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public List<ProductVariant> getProductVariants() {
        return productVariants;
    }

    public void setProductVariants(List<ProductVariant> productVariants) {
        this.productVariants = productVariants;
    }
}
