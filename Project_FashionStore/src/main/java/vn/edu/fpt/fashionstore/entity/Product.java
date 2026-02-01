package vn.edu.fpt.fashionstore.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String size;
    
    @Column(nullable = false)
    private double price;
    
    @Column(nullable = false)
    private String category;
    
    @Column(name = "is_new")
    private boolean isNew;
    
    @Column(name = "is_sale")
    private boolean isSale;

    public Product() {
    }

    public Product(String name, String size, double price, String category, boolean isNew, boolean isSale) {
        this.name = name;
        this.size = size;
        this.price = price;
        this.category = category;
        this.isNew = isNew;
        this.isSale = isSale;
    }

    public Product(long id, String name, String size, double price, String category, boolean isNew, boolean isSale) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.price = price;
        this.category = category;
        this.isNew = isNew;
        this.isSale = isSale;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isSale() {
        return isSale;
    }

    public boolean getIsSale() {
        return isSale;
    }

    public void setSale(boolean sale) {
        isSale = sale;
    }

    public void setIsSale(boolean sale) {
        isSale = sale;
    }

    public boolean isNew() {
        return isNew;
    }

    public boolean getIsNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public void setIsNew(boolean aNew) {
        isNew = aNew;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
