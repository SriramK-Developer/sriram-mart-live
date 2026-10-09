package com.srirammart.web;

import com.srirammart.config.AppUserDetails;
import com.srirammart.model.Category;
import com.srirammart.model.Product;
import com.srirammart.model.Role;
import com.srirammart.model.User;
import com.srirammart.service.CatalogService;
import com.srirammart.service.ListingParams;
import com.srirammart.service.ListingView;
import com.srirammart.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

/** Category pages, search results, seller stores and the full deals list. */
@Controller
public class CatalogController {
    private static final Set<String> SORTS = Set.of("popularity", "price_asc", "price_desc", "rating", "newest");
    private final CatalogService catalog;
    private final UserService users;

    public CatalogController(CatalogService catalog, UserService users) {
        this.catalog = catalog;
        this.users = users;
    }

    @InitBinder("lp")
    void binder(WebDataBinder b) { b.setDisallowedFields("category", "seller", "facets", "dealsOnly"); }

    @GetMapping("/category/{slug}")
    public String category(@PathVariable String slug, @ModelAttribute("lp") ListingParams lp, HttpServletRequest req, Model model) {
        Category c = catalog.category(slug);
        if (c == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        lp.setCategory(c);
        model.addAttribute("category", c);
        model.addAttribute("offerPct", catalog.bannerOffers().getOrDefault(c.getId(), 10));
        model.addAttribute("chips", catalog.view("/category/" + slug, blank(c)).getGroups().stream().filter(g -> g.getParam().equals("sub")).findFirst().orElse(null));
        return render("/category/" + slug, c.getName(), c.getTagline(), lp, req, model);
    }

    @GetMapping("/search")
    public String search(@ModelAttribute("lp") ListingParams lp, HttpServletRequest req, Model model) {
        String q = lp.getQ() == null ? "" : lp.getQ().trim();
        if (q.isEmpty()) return "redirect:/";
        if (q.length() > 80) q = q.substring(0, 80);
        lp.setQ(q);
        return render("/search", "Search results for “" + q + "”", "Showing products that match your search.", lp, req, model);
    }

    @GetMapping("/store/{id}")
    public String store(@PathVariable Long id, @ModelAttribute("lp") ListingParams lp, HttpServletRequest req, Model model) {
        User seller;
        try { seller = users.get(id); } catch (RuntimeException e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND); }
        if (seller.getRole() != Role.SELLER || !seller.isApproved()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        lp.setSeller(seller);
        model.addAttribute("store", seller);
        String name = seller.getStoreName() == null ? seller.getFullName() : seller.getStoreName();
        return render("/store/" + id, name, "Official store on SriramMart", lp, req, model);
    }

    @GetMapping("/deals/all")
    public String allDeals(@ModelAttribute("lp") ListingParams lp, HttpServletRequest req, Model model) {
        lp.setDealsOnly(true);
        return render("/deals/all", "All Deals", "Every discounted product in one place.", lp, req, model);
    }

    private static ListingParams blank(Category c) {
        ListingParams p = new ListingParams();
        p.setCategory(c);
        return p;
    }

    private String render(String path, String heading, String sub, ListingParams lp, HttpServletRequest req, Model model) {
        if (!SORTS.contains(lp.getSort())) lp.setSort("popularity");
        if (lp.getPage() < 0) lp.setPage(0);
        if (lp.getQ() != null && lp.getQ().length() > 80) lp.setQ(lp.getQ().substring(0, 80));
        if (lp.getSub().size() > 20) lp.setSub(new ArrayList<>(lp.getSub().subList(0, 20)));
        if (lp.getBrand().size() > 20) lp.setBrand(new ArrayList<>(lp.getBrand().subList(0, 20)));
        for (Map.Entry<String, String[]> e : req.getParameterMap().entrySet()) {
            if (!e.getKey().startsWith("f_") || e.getKey().length() <= 2 || e.getKey().length() > 40) continue;
            List<String> vals = new ArrayList<>();
            for (String v : Arrays.copyOf(e.getValue(), Math.min(e.getValue().length, 10))) if (!v.isBlank() && v.length() <= 60) vals.add(v);
            if (!vals.isEmpty()) lp.getFacets().put(e.getKey().substring(2), vals);
        }
        Page<Product> page = catalog.list(lp);
        if (lp.getPage() > 0 && page.getTotalPages() > 0 && lp.getPage() >= page.getTotalPages()) {
            lp.setPage(page.getTotalPages() - 1);
            page = catalog.list(lp);
        }
        ListingView view = catalog.view(path, lp);
        view.setTotalPages(page.getTotalPages());
        view.setTotalItems(page.getTotalElements());
        view.setFrom(page.getNumberOfElements() == 0 ? 0 : lp.getPage() * CatalogService.PAGE_SIZE + 1);
        view.setTo(lp.getPage() * CatalogService.PAGE_SIZE + page.getNumberOfElements());
        model.addAttribute("view", view);
        model.addAttribute("products", page.getContent());
        model.addAttribute("heading", heading);
        model.addAttribute("subheading", sub);
        model.addAttribute("pageTitle", heading);
        return "listing";
    }
}
