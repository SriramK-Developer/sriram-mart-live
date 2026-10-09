package com.srirammart.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Everything a listing template needs beyond the product page itself. */
public class ListingView {
    private final String path;
    private final ListingParams params;
    private List<FacetGroup> groups = new ArrayList<>();
    private List<PriceBucket> priceBuckets = new ArrayList<>();
    private int totalPages;
    private long totalItems;
    private int from;
    private int to;

    public ListingView(String path, ListingParams params) { this.path = path; this.params = params; }

    /** Builds the URL for another page (or with page 0) while keeping every active filter. */
    public String url(int page) {
        StringBuilder sb = new StringBuilder(path);
        char sep = '?';
        if (params.getQ() != null && !params.getQ().isBlank()) { sb.append(sep).append("q=").append(enc(params.getQ())); sep = '&'; }
        for (String v : params.getSub()) { sb.append(sep).append("sub=").append(enc(v)); sep = '&'; }
        for (String v : params.getBrand()) { sb.append(sep).append("brand=").append(enc(v)); sep = '&'; }
        if (params.getPrice() != null) { sb.append(sep).append("price=").append(enc(params.getPrice())); sep = '&'; }
        if (params.getRating() != null) { sb.append(sep).append("rating=").append(params.getRating()); sep = '&'; }
        for (Map.Entry<String, List<String>> e : params.getFacets().entrySet())
            for (String v : e.getValue()) { sb.append(sep).append(enc("f_" + e.getKey())).append('=').append(enc(v)); sep = '&'; }
        if (!"popularity".equals(params.getSort())) { sb.append(sep).append("sort=").append(enc(params.getSort())); sep = '&'; }
        if (page > 0) { sb.append(sep).append("page=").append(page); }
        return sb.toString();
    }

    public String getClearUrl() {
        return params.getQ() != null && !params.getQ().isBlank() ? path + "?q=" + enc(params.getQ()) : path;
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    public String getPath() { return path; }
    public ListingParams getParams() { return params; }
    public List<FacetGroup> getGroups() { return groups; }
    public void setGroups(List<FacetGroup> g) { this.groups = g; }
    public List<PriceBucket> getPriceBuckets() { return priceBuckets; }
    public void setPriceBuckets(List<PriceBucket> p) { this.priceBuckets = p; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int v) { this.totalPages = v; }
    public long getTotalItems() { return totalItems; }
    public void setTotalItems(long v) { this.totalItems = v; }
    public int getFrom() { return from; }
    public void setFrom(int v) { this.from = v; }
    public int getTo() { return to; }
    public void setTo(int v) { this.to = v; }
    public int getCurrentPage() { return params.getPage(); }

    public static class FacetGroup {
        private final String title;
        private final String param;
        private final List<FacetValue> values = new ArrayList<>();
        public FacetGroup(String title, String param) { this.title = title; this.param = param; }
        public String getTitle() { return title; }
        public String getParam() { return param; }
        public List<FacetValue> getValues() { return values; }
    }

    public static class FacetValue {
        private final String value;
        private final long count;
        private final boolean checked;
        public FacetValue(String value, long count, boolean checked) { this.value = value; this.count = count; this.checked = checked; }
        public String getValue() { return value; }
        public long getCount() { return count; }
        public boolean isChecked() { return checked; }
    }

    public static class PriceBucket {
        private final String label;
        private final String value;
        private final boolean checked;
        public PriceBucket(String label, String value, boolean checked) { this.label = label; this.value = value; this.checked = checked; }
        public String getLabel() { return label; }
        public String getValue() { return value; }
        public boolean isChecked() { return checked; }
    }
}
