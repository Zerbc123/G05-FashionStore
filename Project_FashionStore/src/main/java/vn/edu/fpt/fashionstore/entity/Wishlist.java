package vn.edu.fpt.fashionstore.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Wishlist")
public class Wishlist {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wishlist_id")
    private Integer wishlistId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "added_date")
    private LocalDateTime addedDate;
    
    // Constructors
    public Wishlist() {
        this.addedDate = LocalDateTime.now();
    }
    
    public Wishlist(Customer customer, Product product) {
        this.customer = customer;
        this.product = product;
        this.addedDate = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Integer getWishlistId() {
        return wishlistId;
    }
    
    public void setWishlistId(Integer wishlistId) {
        this.wishlistId = wishlistId;
    }
    
    public Customer getCustomer() {
        return customer;
    }
    
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
    
    public Product getProduct() {
        return product;
    }
    
    public void setProduct(Product product) {
        this.product = product;
    }
    
    public LocalDateTime getAddedDate() {
        return addedDate;
    }
    
    public void setAddedDate(LocalDateTime addedDate) {
        this.addedDate = addedDate;
    }
}
