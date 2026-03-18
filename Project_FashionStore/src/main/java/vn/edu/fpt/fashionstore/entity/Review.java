package vn.edu.fpt.fashionstore.entity;
 
import jakarta.persistence.*;
import java.time.LocalDate;
 
@Entity
@Table(name = "Review")
public class Review {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
 
    @Column(name = "rating")
    private Integer rating;
 
    @Column(name = "comment", columnDefinition = "NVARCHAR(MAX)")
    private String comment;
 
    @Column(name = "review_date")
    private LocalDate reviewDate;
 
    @Column(name = "is_active")
    private Boolean isActive;
 
    // --- CONSTRUCTORS ---
    public Review() {
        this.reviewDate = LocalDate.now();
        this.isActive = true; // Mặc định khi khách đánh giá là cho hiển thị luôn
    }
 
    // --- GETTERS & SETTERS ---
    public Long getReviewId() {
        return reviewId;
    }
 
    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
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
 
    public Integer getRating() {
        return rating;
    }
 
    public void setRating(Integer rating) {
        this.rating = rating;
    }
 
    public String getComment() {
        return comment;
    }
 
    public void setComment(String comment) {
        this.comment = comment;
    }
 
    public LocalDate getReviewDate() {
        return reviewDate;
    }
 
    public void setReviewDate(LocalDate reviewDate) {
        this.reviewDate = reviewDate;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}