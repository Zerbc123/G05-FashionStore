package vn.edu.fpt.fashionstore.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Banners")
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "banner_id")
    private Long bannerId;

    @Column(name = "title", length = 255)
    private String title; // Tiêu đề của banner (VD: Sale 8/3)

    @Column(name = "image_url", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String imageUrl; // Link ảnh (bắt buộc phải có)

    @Column(name = "target_link", length = 255)
    private String targetLink; // Click vào ảnh thì nhảy đi đâu? (VD: /products/sale)

    @Column(name = "display_order")
    private Integer displayOrder; // Thứ tự xuất hiện (1, 2, 3...)

    @Column(name = "is_active")
    private Boolean isActive; // Trạng thái Bật/Tắt

    // --- CONSTRUCTORS ---
    public Banner() {
        this.isActive = true; // Mặc định tạo ra là được bật
        this.displayOrder = 0;
    }

    public Banner(String title, String imageUrl, String targetLink, Integer displayOrder, Boolean isActive) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.targetLink = targetLink;
        this.displayOrder = displayOrder;
        this.isActive = isActive;
    }

    // --- GETTERS & SETTERS ---
    public Long getBannerId() { return bannerId; }
    public void setBannerId(Long bannerId) { this.bannerId = bannerId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getTargetLink() { return targetLink; }
    public void setTargetLink(String targetLink) { this.targetLink = targetLink; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}