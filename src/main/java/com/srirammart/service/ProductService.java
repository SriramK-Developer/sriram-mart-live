package com.srirammart.service;

import com.srirammart.model.Category;
import com.srirammart.model.Product;
import com.srirammart.model.Role;
import com.srirammart.model.User;
import com.srirammart.repo.CategoryRepository;
import com.srirammart.repo.ProductRepository;
import com.srirammart.repo.UserRepository;
import com.srirammart.web.ProductForm;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Product create/update for sellers and admins, plus bulk CSV import. */
@Service
public class ProductService {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final UserRepository users;

    public ProductService(ProductRepository products, CategoryRepository categories, UserRepository users) {
        this.products = products;
        this.categories = categories;
        this.users = users;
    }

    public static class ImportResult {
        private int created;
        private int updated;
        private final List<String> errors = new ArrayList<>();
        public int getCreated() { return created; }
        public int getUpdated() { return updated; }
        public List<String> getErrors() { return errors; }
    }

    public ProductForm toForm(Product p) {
        ProductForm f = new ProductForm();
        f.setId(p.getId());
        f.setName(p.getName());
        f.setSubtitle(p.getSubtitle());
        f.setBrand(p.getBrand());
        f.setCategorySlug(p.getCategory().getSlug());
        f.setSubCategory(p.getSubCategory());
        f.setPrice(p.getPrice().stripTrailingZeros().toPlainString());
        f.setMrp(p.getMrp().stripTrailingZeros().toPlainString());
        f.setStock(p.getStock());
        f.setDescription(p.getDescription());
        f.setActive(p.isActive());
        StringBuilder hl = new StringBuilder();
        for (String[] h : p.getHighlightList()) hl.append(h[0]).append(" | ").append(h[1]).append('\n');
        f.setHighlights(hl.toString().trim());
        StringBuilder sp = new StringBuilder();
        for (Map.Entry<String, String> e : p.getSpecMap().entrySet()) sp.append(e.getKey()).append(": ").append(e.getValue()).append('\n');
        f.setSpecs(sp.toString().trim());
        return f;
    }

    /**
     * Creates or updates a product. Sellers can only touch their own products; admins pass the owning seller.
     * @return list of validation errors (empty when saved); the saved product is returned through {@code out[0]}
     */
    @Transactional
    public List<String> save(ProductForm f, String newImagePath, User actor, User ownerIfAdmin, Product[] out) {
        List<String> errors = new ArrayList<>();
        if (f.getName() == null || f.getName().trim().length() < 3) errors.add("Enter a product name (at least 3 characters).");
        Category cat = f.getCategorySlug() == null ? null : categories.findBySlug(f.getCategorySlug()).orElse(null);
        if (cat == null) errors.add("Choose a category.");
        BigDecimal price = money(f.getPrice()), mrp = money(f.getMrp());
        if (price == null || price.signum() <= 0) errors.add("Enter a selling price greater than 0.");
        if (mrp == null || mrp.signum() <= 0) mrp = price;
        if (price != null && mrp != null && mrp.compareTo(price) < 0) errors.add("MRP cannot be lower than the selling price.");
        if (f.getStock() == null || f.getStock() < 0 || f.getStock() > 1_000_000) errors.add("Enter stock between 0 and 1,000,000.");
        if (!errors.isEmpty()) return errors;

        Product p;
        if (f.getId() != null) {
            p = products.findById(f.getId()).orElse(null);
            if (p == null) return List.of("Product not found.");
            if (actor.getRole() == Role.SELLER && !p.getSeller().getId().equals(actor.getId())) return List.of("You can only edit your own products.");
        } else {
            p = new Product();
            p.setSku(makeSku(f.getName()));
            p.setSeller(actor.getRole() == Role.SELLER ? actor : ownerIfAdmin);
            if (p.getSeller() == null) return List.of("Choose the seller for this product.");
            p.setImageMain("/img/no-image.svg");
        }
        p.setName(f.getName().trim());
        p.setSubtitle(blankToNull(f.getSubtitle()));
        p.setBrand(blankToNull(f.getBrand()));
        p.setCategory(cat);
        p.setSubCategory(blankToNull(f.getSubCategory()));
        p.setPrice(price.setScale(2, RoundingMode.HALF_UP));
        p.setMrp(mrp.setScale(2, RoundingMode.HALF_UP));
        p.setStock(f.getStock());
        p.setDescription(blankToNull(f.getDescription()));
        p.setHighlights(parseHighlights(f.getHighlights()));
        p.setSpecs(parseSpecs(f.getSpecs()));
        p.setActive(f.isActive());
        if (newImagePath != null) p.setImageMain(newImagePath);
        p = products.save(p);
        if (out != null && out.length > 0) out[0] = p;
        return errors;
    }

    @Transactional
    public void setStock(Long id, int stock, User actor) {
        Product p = products.findById(id).orElseThrow();
        if (actor.getRole() == Role.SELLER && !p.getSeller().getId().equals(actor.getId())) return;
        p.setStock(Math.max(0, Math.min(1_000_000, stock)));
        products.save(p);
    }

    @Transactional
    public void toggleActive(Long id, User actor) {
        Product p = products.findById(id).orElseThrow();
        if (actor.getRole() == Role.SELLER && !p.getSeller().getId().equals(actor.getId())) return;
        p.setActive(!p.isActive());
        products.save(p);
    }

    // ---------------------------------------------------------------- bulk import
    /** Imports rows shaped like data/products.csv. Existing SKUs are updated, new SKUs created. */
    @Transactional
    public ImportResult importRows(List<Map<String, String>> rows) {
        ImportResult res = new ImportResult();
        for (Map<String, String> r : rows) {
            String line = "Line " + r.getOrDefault("_line", "?") + ": ";
            try {
                String sku = get(r, "sku");
                String name = get(r, "name");
                if (sku.isEmpty() || name.isEmpty()) { res.errors.add(line + "sku and name are required."); continue; }
                Category cat = categories.findBySlug(get(r, "category")).orElse(null);
                if (cat == null) { res.errors.add(line + "unknown category '" + get(r, "category") + "'."); continue; }
                User seller = users.findByUsernameIgnoreCase(get(r, "seller")).orElse(null);
                if (seller == null || seller.getRole() != Role.SELLER) { res.errors.add(line + "seller '" + get(r, "seller") + "' is not a seller username."); continue; }
                BigDecimal price = money(get(r, "price"));
                if (price == null || price.signum() <= 0) { res.errors.add(line + "invalid price."); continue; }
                BigDecimal mrp = money(get(r, "mrp"));
                if (mrp == null || mrp.compareTo(price) < 0) mrp = price;

                Product p = products.findBySku(sku).orElse(null);
                boolean isNew = p == null;
                if (isNew) p = new Product();
                p.setSku(sku);
                p.setName(name);
                p.setSubtitle(blankToNull(get(r, "subtitle")));
                p.setBrand(blankToNull(get(r, "brand")));
                p.setCategory(cat);
                p.setSubCategory(blankToNull(get(r, "subCategory")));
                p.setSeller(seller);
                p.setPrice(price.setScale(2, RoundingMode.HALF_UP));
                p.setMrp(mrp.setScale(2, RoundingMode.HALF_UP));
                p.setStock(parseInt(get(r, "stock"), 0));
                p.setRating(Math.max(0, Math.min(5, parseDouble(get(r, "rating"), 4.0))));
                p.setRatingCount(parseInt(get(r, "ratingCount"), 0));
                p.setDescription(blankToNull(get(r, "description")));
                p.setHighlights(blankToNull(get(r, "highlights")));
                p.setSpecs(blankToNull(get(r, "specs")));
                String img = get(r, "image");
                p.setImageMain(img.isEmpty() ? (p.getImageMain() != null ? p.getImageMain() : "/img/no-image.svg") : img);
                p.setGallery(blankToNull(get(r, "gallery")));
                p.setFeatured("true".equalsIgnoreCase(get(r, "featured")));
                p.setActive(!"false".equalsIgnoreCase(get(r, "active")));
                products.save(p);
                if (isNew) res.created++; else res.updated++;
            } catch (RuntimeException ex) {
                res.errors.add(line + "could not import (" + ex.getClass().getSimpleName() + ").");
            }
        }
        return res;
    }

    // ---------------------------------------------------------------- helpers
    private static String get(Map<String, String> r, String k) { String v = r.get(k); return v == null ? "" : v.trim(); }
    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
    private static int parseInt(String s, int d) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return d; } }
    private static double parseDouble(String s, double d) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return d; } }

    private static BigDecimal money(String s) {
        if (s == null || s.isBlank()) return null;
        try { return new BigDecimal(s.replace(",", "").replace("₹", "").trim()); } catch (NumberFormatException e) { return null; }
    }

    private static String makeSku(String name) {
        String base = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.length() > 80) base = base.substring(0, 80);
        return base + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    /** "Title | text" per line -> "Title|text~Title|text" */
    private static String parseHighlights(String raw) {
        if (raw == null || raw.isBlank()) return null;
        List<String> out = new ArrayList<>();
        for (String line : raw.split("\\r?\\n")) {
            int i = line.indexOf('|');
            if (i > 0 && i < line.length() - 1) out.add(clean(line.substring(0, i)) + "|" + clean(line.substring(i + 1)));
        }
        return out.isEmpty() ? null : String.join("~", out);
    }

    /** "Key: Value" per line -> "|Key:Value|Key:Value|" */
    private static String parseSpecs(String raw) {
        if (raw == null || raw.isBlank()) return null;
        StringBuilder sb = new StringBuilder("|");
        boolean any = false;
        for (String line : raw.split("\\r?\\n")) {
            int i = line.indexOf(':');
            if (i > 0 && i < line.length() - 1) { sb.append(clean(line.substring(0, i))).append(':').append(clean(line.substring(i + 1))).append('|'); any = true; }
        }
        return any ? sb.toString() : null;
    }

    private static String clean(String s) { return s.replace("|", "/").replace("~", "-").replace(":", " -").trim(); }
}
