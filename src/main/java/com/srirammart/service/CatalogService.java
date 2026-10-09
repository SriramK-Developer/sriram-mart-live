package com.srirammart.service;

import com.srirammart.model.Category;
import com.srirammart.model.Product;
import com.srirammart.model.User;
import com.srirammart.repo.CategoryRepository;
import com.srirammart.repo.ProductRepository;
import com.srirammart.util.Fmt;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {
    public static final int PAGE_SIZE = 20;
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final Fmt fmt;

    public CatalogService(ProductRepository products, CategoryRepository categories, Fmt fmt) {
        this.products = products;
        this.categories = categories;
        this.fmt = fmt;
    }

    public List<Category> categories() { return categories.findAllByOrderByIdAsc(); }
    public Category category(String slug) { return categories.findBySlug(slug).orElse(null); }

    // ------------------------------------------------------------------ listing
    public Page<Product> list(ListingParams lp) {
        Pageable pg = PageRequest.of(Math.max(0, lp.getPage()), PAGE_SIZE, sort(lp.getSort()));
        return products.findAll(spec(lp, true), pg);
    }

    private Sort sort(String s) {
        if (s == null) s = "popularity";
        return switch (s) {
            case "price_asc" -> Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"));
            case "price_desc" -> Sort.by(Sort.Order.desc("price"), Sort.Order.asc("id"));
            case "rating" -> Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("ratingCount"), Sort.Order.asc("id"));
            case "newest" -> Sort.by(Sort.Order.desc("id"));
            default -> Sort.by(Sort.Order.desc("ratingCount"), Sort.Order.asc("id"));
        };
    }

    private Specification<Product> spec(ListingParams lp, boolean withFilters) {
        return (root, cq, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.isTrue(root.get("active")));
            if (lp.getCategory() != null) ps.add(cb.equal(root.get("category"), lp.getCategory()));
            if (lp.getSeller() != null) ps.add(cb.equal(root.get("seller"), lp.getSeller()));
            if (lp.isDealsOnly()) ps.add(cb.greaterThan(root.<BigDecimal>get("mrp"), root.<BigDecimal>get("price")));
            if (lp.getQ() != null && !lp.getQ().isBlank()) {
                String cleaned = lp.getQ().toLowerCase(Locale.ROOT).replace("%", " ").replace("_", " ").trim();
                int used = 0;
                for (String tok : cleaned.split("\\s+")) {
                    if (tok.isEmpty() || used++ >= 6) continue;
                    String like = "%" + tok + "%";
                    ps.add(cb.or(
                            cb.like(cb.lower(root.<String>get("name")), like),
                            cb.like(cb.lower(root.<String>get("brand")), like),
                            cb.like(cb.lower(root.<String>get("subCategory")), like),
                            cb.like(cb.lower(root.<String>get("subtitle")), like)));
                }
            }
            if (withFilters) {
                if (!lp.getSub().isEmpty()) ps.add(root.get("subCategory").in(lp.getSub()));
                if (!lp.getBrand().isEmpty()) ps.add(root.get("brand").in(lp.getBrand()));
                ListingParams.BigDecimalRange r = lp.priceRange();
                if (r != null) {
                    if (r.min != null) ps.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("price"), BigDecimal.valueOf(r.min)));
                    if (r.max != null) ps.add(cb.lessThan(root.<BigDecimal>get("price"), BigDecimal.valueOf(r.max)));
                }
                if (lp.getRating() != null) ps.add(cb.greaterThanOrEqualTo(root.<Double>get("rating"), lp.getRating()));
                for (Map.Entry<String, List<String>> e : lp.getFacets().entrySet()) {
                    List<Predicate> any = new ArrayList<>();
                    for (String v : e.getValue()) any.add(cb.like(root.<String>get("specs"), "%|" + e.getKey() + ":" + v + "|%"));
                    if (!any.isEmpty()) ps.add(cb.or(any.toArray(new Predicate[0])));
                }
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    /** Builds the filter sidebar (sub-category, brand, category specific facets, price ranges) for a listing. */
    public ListingView view(String path, ListingParams lp) {
        ListingView v = new ListingView(path, lp);
        List<Product> universe = products.findAll(spec(lp, false));
        List<ListingView.FacetGroup> groups = new ArrayList<>();

        Map<String, Long> subs = new LinkedHashMap<>(), brands = new LinkedHashMap<>();
        for (Product p : universe) {
            if (p.getSubCategory() != null) subs.merge(p.getSubCategory(), 1L, Long::sum);
            if (p.getBrand() != null) brands.merge(p.getBrand(), 1L, Long::sum);
        }
        groups.add(group(lp.getCategory() != null ? "Category" : "Sub-category", "sub", subs, lp.getSub()));
        groups.add(group("Brand", "brand", brands, lp.getBrand()));
        if (lp.getCategory() != null) {
            for (String key : lp.getCategory().getFacetKeys()) {
                Map<String, Long> counts = new TreeMap<>();
                for (Product p : universe) {
                    String val = p.getSpecMap().get(key);
                    if (val != null && !val.isBlank()) counts.merge(val, 1L, Long::sum);
                }
                if (counts.size() > 1) groups.add(group(key, "f_" + key, counts, lp.getFacets().getOrDefault(key, List.of())));
            }
        }
        groups.removeIf(g -> g.getValues().isEmpty());
        v.setGroups(groups);

        List<Integer> steps = lp.getCategory() != null && !lp.getCategory().getStepValues().isEmpty()
                ? lp.getCategory().getStepValues() : List.of(500, 1000, 5000, 20000);
        List<ListingView.PriceBucket> buckets = new ArrayList<>();
        buckets.add(new ListingView.PriceBucket("Under " + fmt.inr(steps.get(0)), "0-" + steps.get(0), ("0-" + steps.get(0)).equals(lp.getPrice())));
        for (int i = 0; i + 1 < steps.size(); i++) {
            String val = steps.get(i) + "-" + steps.get(i + 1);
            buckets.add(new ListingView.PriceBucket(fmt.inr(steps.get(i)) + " – " + fmt.inr(steps.get(i + 1)), val, val.equals(lp.getPrice())));
        }
        int last = steps.get(steps.size() - 1);
        buckets.add(new ListingView.PriceBucket("Above " + fmt.inr(last), last + "-", (last + "-").equals(lp.getPrice())));
        v.setPriceBuckets(buckets);
        return v;
    }

    private ListingView.FacetGroup group(String title, String param, Map<String, Long> counts, List<String> selected) {
        ListingView.FacetGroup g = new ListingView.FacetGroup(title, param);
        counts.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .forEach(e -> g.getValues().add(new ListingView.FacetValue(e.getKey(), e.getValue(), selected.contains(e.getKey()))));
        return g;
    }

    // ------------------------------------------------------------------ misc lookups
    public Product product(Long id) { return products.findById(id).orElse(null); }
    public List<Product> featured() { return products.findByFeaturedTrueAndActiveTrueOrderByIdAsc(); }
    public List<Product> topDeals(int n) { return products.findTopDeals(PageRequest.of(0, n)); }
    public List<Product> related(Product p, int n) { return products.findRelated(p.getCategory(), p.getId(), PageRequest.of(0, n)); }
    public List<Product> productsOf(Category c) { return products.findByActiveTrueAndCategoryOrderByIdAsc(c); }

    public List<Product> suggest(String q, int n) {
        ListingParams lp = new ListingParams();
        lp.setQ(q);
        return products.findAll(spec(lp, false), PageRequest.of(0, n, sort("popularity"))).getContent();
    }

    /** Highest discount per category id, floored to a multiple of 5 for banner copy such as "Up to 40% OFF". */
    public Map<Long, Integer> bannerOffers() {
        Map<Long, Integer> out = new LinkedHashMap<>();
        for (Object[] row : products.discountedPrices()) {
            long id = ((Number) row[0]).longValue();
            BigDecimal mrp = (BigDecimal) row[1], price = (BigDecimal) row[2];
            if (mrp.signum() <= 0) continue;
            int pct = mrp.subtract(price).multiply(BigDecimal.valueOf(100)).divide(mrp, 0, java.math.RoundingMode.FLOOR).intValue();
            out.merge(id, pct, Math::max);
        }
        out.replaceAll((k, v) -> Math.max(5, v / 5 * 5));
        for (Category c : categories.findAll()) out.putIfAbsent(c.getId(), 5);
        return out;
    }

    public List<Product> sellerProducts(User seller) { return products.findBySellerOrderByIdDesc(seller); }
}
