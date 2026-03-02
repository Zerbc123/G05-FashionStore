package vn.edu.fpt.fashionstore.entity;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Entity
@Table(name = "Voucher")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "voucher_id")
    private Integer voucherId;

    @Column(name = "code", length = 50, nullable = false)
    private String code;

    @Column(name = "discount_value")
    private Double discountValue;

    @Column(name = "expired_date")
    @Temporal(TemporalType.DATE)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date expiredDate;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "min_order_value")
    private Double minOrderValue;

    // --- CONSTRUCTORS ---
    public Voucher() {
    }

    public Voucher(Integer voucherId, String code, Double discountValue, Date expiredDate, Boolean isActive, Double minOrderValue) {
        this.voucherId = voucherId;
        this.code = code;
        this.discountValue = discountValue;
        this.expiredDate = expiredDate;
        this.isActive = isActive;
        this.minOrderValue = minOrderValue;
    }

    // --- GETTERS AND SETTERS ---
    public Integer getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Integer voucherId) {
        this.voucherId = voucherId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Double getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(Double discountValue) {
        this.discountValue = discountValue;
    }

    public Date getExpiredDate() {
        return expiredDate;
    }

    public void setExpiredDate(Date expiredDate) {
        this.expiredDate = expiredDate;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Double getMinOrderValue() {
        return minOrderValue;
    }

    public void setMinOrderValue(Double minOrderValue) {
        this.minOrderValue = minOrderValue;
    }
}