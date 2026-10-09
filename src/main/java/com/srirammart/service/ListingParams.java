package com.srirammart.service;

import com.srirammart.model.Category;
import com.srirammart.model.User;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Filters/sort/page for product listing screens. Bound from request parameters. */
public class ListingParams {
    private String q;
    private List<String> sub = new ArrayList<>();
    private List<String> brand = new ArrayList<>();
    private String price;
    private String sort = "popularity";
    private int page = 0;
    private Double rating;
    private Map<String, List<String>> facets = new LinkedHashMap<>();
    private Category category;
    private User seller;
    private boolean dealsOnly;

    public BigDecimalRange priceRange() {
        if (price == null || !price.matches("\\d*-\\d*") || price.equals("-")) return null;
        String[] p = price.split("-", -1);
        Integer min = p[0].isEmpty() ? null : Integer.valueOf(p[0]);
        Integer max = p.length < 2 || p[1].isEmpty() ? null : Integer.valueOf(p[1]);
        return new BigDecimalRange(min, max);
    }

    public boolean hasFilters() {
        return !sub.isEmpty() || !brand.isEmpty() || price != null || rating != null || !facets.isEmpty();
    }

    public static class BigDecimalRange {
        public final Integer min;
        public final Integer max;
        public BigDecimalRange(Integer min, Integer max) { this.min = min; this.max = max; }
    }

        public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }
    public List<String> getSub() { return sub; }
    public void setSub(List<String> sub) { this.sub = sub; }
    public List<String> getBrand() { return brand; }
    public void setBrand(List<String> brand) { this.brand = brand; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public Map<String, List<String>> getFacets() { return facets; }
    public void setFacets(Map<String, List<String>> facets) { this.facets = facets; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public User getSeller() { return seller; }
    public void setSeller(User seller) { this.seller = seller; }
    public boolean isDealsOnly() { return dealsOnly; }
    public void setDealsOnly(boolean dealsOnly) { this.dealsOnly = dealsOnly; }
}
