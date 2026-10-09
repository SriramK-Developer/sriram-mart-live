package com.srirammart.web;

public class ProductForm {
    private Long id;
    private String name;
    private String subtitle;
    private String brand;
    private String categorySlug;
    private String subCategory;
    private String price;
    private String mrp;
    private Integer stock;
    private String description;
    private String highlights;
    private String specs;
    private boolean active = true;

        public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getCategorySlug() { return categorySlug; }
    public void setCategorySlug(String categorySlug) { this.categorySlug = categorySlug; }
    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    public String getMrp() { return mrp; }
    public void setMrp(String mrp) { this.mrp = mrp; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getHighlights() { return highlights; }
    public void setHighlights(String highlights) { this.highlights = highlights; }
    public String getSpecs() { return specs; }
    public void setSpecs(String specs) { this.specs = specs; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
