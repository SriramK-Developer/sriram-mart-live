package com.srirammart.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_category", columnList = "category_id"),
        @Index(name = "idx_product_seller", columnList = "seller_id")})
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 120)
    private String sku;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(length = 120)
    private String subtitle;
    @Column(length = 80)
    private String brand;
    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id")
    private Category category;
    @Column(length = 80)
    private String subCategory;
    @ManyToOne(optional = false)
    @JoinColumn(name = "seller_id")
    private User seller;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal mrp;
    @Column(nullable = false)
    private int stock;
    @Column(length = 2000)
    private String description;
    /** Highlights: "Title|Text~Title|Text" */
    @Column(length = 3000)
    private String highlights;
    /** Specs: "|Key:Value|Key:Value|" */
    @Column(length = 3000)
    private String specs;
    @Column(length = 300)
    private String imageMain;
    /** Extra images, comma separated. */
    @Column(length = 600)
    private String gallery;
    @Column(nullable = false)
    private double rating = 4.0;
    @Column(nullable = false)
    private int ratingCount;
    @Column(nullable = false)
    private int soldCount;
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false)
    private boolean featured;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public int getDiscountPercent() {
        if (mrp == null || price == null || mrp.signum() <= 0 || price.compareTo(mrp) >= 0) return 0;
        return mrp.subtract(price).multiply(BigDecimal.valueOf(100)).divide(mrp, 0, RoundingMode.FLOOR).intValue();
    }
    public BigDecimal getSavings() {
        return mrp == null || price == null || price.compareTo(mrp) >= 0 ? BigDecimal.ZERO : mrp.subtract(price);
    }
    public boolean isInStock() { return stock > 0; }
    public boolean isLowStock() { return stock > 0 && stock <= 5; }
    public String getStockLabel() {
        if (stock <= 0) return "Out of Stock";
        if (stock <= 5) return "Only " + stock + " left";
        return "In Stock";
    }
    public List<String> getImages() {
        List<String> out = new ArrayList<>();
        if (imageMain != null && !imageMain.isBlank()) out.add(imageMain);
        if (gallery != null) for (String g : gallery.split(",")) if (!g.isBlank()) out.add(g.trim());
        return out;
    }
    public Map<String, String> getSpecMap() {
        Map<String, String> m = new LinkedHashMap<>();
        if (specs == null) return m;
        for (String part : specs.split("\\|")) {
            int i = part.indexOf(':');
            if (i > 0) m.put(part.substring(0, i).trim(), part.substring(i + 1).trim());
        }
        return m;
    }
    public List<String[]> getHighlightList() {
        List<String[]> out = new ArrayList<>();
        if (highlights == null) return out;
        for (String h : highlights.split("~")) {
            int i = h.indexOf('|');
            if (i > 0) out.add(new String[]{h.substring(0, i).trim(), h.substring(i + 1).trim()});
        }
        return out;
    }

        public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }
    public User getSeller() { return seller; }
    public void setSeller(User seller) { this.seller = seller; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getMrp() { return mrp; }
    public void setMrp(BigDecimal mrp) { this.mrp = mrp; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getHighlights() { return highlights; }
    public void setHighlights(String highlights) { this.highlights = highlights; }
    public String getSpecs() { return specs; }
    public void setSpecs(String specs) { this.specs = specs; }
    public String getImageMain() { return imageMain; }
    public void setImageMain(String imageMain) { this.imageMain = imageMain; }
    public String getGallery() { return gallery; }
    public void setGallery(String gallery) { this.gallery = gallery; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }
    public int getSoldCount() { return soldCount; }
    public void setSoldCount(int soldCount) { this.soldCount = soldCount; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
