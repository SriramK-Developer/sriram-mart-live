package com.srirammart.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 60)
    private String slug;
    @Column(nullable = false, length = 80)
    private String name;
    @Column(length = 30)
    private String icon;
    /** Comma separated spec keys shown as extra filters, e.g. "RAM,Storage". */
    @Column(length = 120)
    private String facets;
    /** Comma separated price steps used for the price range filter. */
    @Column(length = 120)
    private String priceSteps;
    @Column(length = 300)
    private String tagline;
    @Column(length = 120)
    private String eyebrow;
    @Column(length = 120)
    private String title;
    @Column(length = 250)
    private String subline;

    public List<String> getFacetKeys() {
        List<String> out = new ArrayList<>();
        if (facets != null) for (String f : facets.split(",")) if (!f.isBlank()) out.add(f.trim());
        return out;
    }
    public List<Integer> getStepValues() {
        List<Integer> out = new ArrayList<>();
        if (priceSteps != null) for (String f : priceSteps.split(",")) if (!f.isBlank()) out.add(Integer.parseInt(f.trim()));
        return out;
    }

        public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getFacets() { return facets; }
    public void setFacets(String facets) { this.facets = facets; }
    public String getPriceSteps() { return priceSteps; }
    public void setPriceSteps(String priceSteps) { this.priceSteps = priceSteps; }
    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }
    public String getEyebrow() { return eyebrow; }
    public void setEyebrow(String eyebrow) { this.eyebrow = eyebrow; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubline() { return subline; }
    public void setSubline(String subline) { this.subline = subline; }
}
