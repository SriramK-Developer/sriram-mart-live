package com.srirammart.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "coupons")
public class Coupon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 40)
    private String code;
    @Column(length = 200)
    private String description;
    /** Percentage off (0 when a flat amount is used). */
    private int discountPercent;
    @Column(precision = 12, scale = 2)
    private BigDecimal flatAmount = BigDecimal.ZERO;
    @Column(precision = 12, scale = 2)
    private BigDecimal maxDiscount = BigDecimal.ZERO;
    @Column(precision = 12, scale = 2)
    private BigDecimal minOrder = BigDecimal.ZERO;
    private LocalDate expiresOn;
    private boolean active = true;
    private int usedCount;

    public boolean isExpired() { return expiresOn != null && expiresOn.isBefore(LocalDate.now()); }

        public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(int discountPercent) { this.discountPercent = discountPercent; }
    public BigDecimal getFlatAmount() { return flatAmount; }
    public void setFlatAmount(BigDecimal flatAmount) { this.flatAmount = flatAmount; }
    public BigDecimal getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(BigDecimal maxDiscount) { this.maxDiscount = maxDiscount; }
    public BigDecimal getMinOrder() { return minOrder; }
    public void setMinOrder(BigDecimal minOrder) { this.minOrder = minOrder; }
    public LocalDate getExpiresOn() { return expiresOn; }
    public void setExpiresOn(LocalDate expiresOn) { this.expiresOn = expiresOn; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getUsedCount() { return usedCount; }
    public void setUsedCount(int usedCount) { this.usedCount = usedCount; }
}
