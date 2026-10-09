package com.srirammart.web;

import com.srirammart.config.AppUserDetails;
import com.srirammart.model.Category;
import com.srirammart.model.Product;
import com.srirammart.model.Role;
import com.srirammart.model.User;
import com.srirammart.service.CatalogService;
import com.srirammart.service.CouponService;
import com.srirammart.service.OrderService;
import com.srirammart.service.UserService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final CatalogService catalog;
    private final UserService users;
    private final OrderService orders;
    private final CouponService coupons;

    public HomeController(CatalogService catalog, UserService users, OrderService orders, CouponService coupons) {
        this.catalog = catalog;
        this.users = users;
        this.orders = orders;
        this.coupons = coupons;
    }

    @GetMapping("/")
    public String home(@AuthenticationPrincipal AppUserDetails me, Model model) {
        if (me != null && me.getRole() == Role.ADMIN) return "redirect:/admin";
        if (me != null && me.getRole() == Role.SELLER) return "redirect:/seller";
        List<Product> featured = catalog.featured();
        if (featured.isEmpty()) featured = catalog.topDeals(5);
        model.addAttribute("featured", featured);
        model.addAttribute("coupons", coupons.active());
        if (me != null) {
            User u = users.get(me);
            model.addAttribute("orderCount", orders.count(u));
        }
        List<String[]> picks = new ArrayList<>();
        picks.add(new String[]{"Laptops", "for Work & Play", "/category/computers-accessories?sub=Laptops", "/img/products/asus-vivobook-15.jpg", "blue"});
        picks.add(new String[]{"Smartphones", "Stay Connected", "/category/mobiles-tablets?sub=Smartphones", "/img/products/samsung-galaxy-m35-5g.jpg", "teal"});
        picks.add(new String[]{"Home Essentials", "For a Better Tomorrow", "/category/home-living", "/img/products/philips-air-fryer.jpg", "amber"});
        picks.add(new String[]{"Fashion Trends", "Look Good, Feel Good", "/category/fashion", "/img/products/nike-mens-air-force-1-sneakers.jpg", "rose"});
        picks.add(new String[]{"Sports & Fitness", "Stay Active, Stay Healthy", "/category/sports-fitness", "/img/products/adjustable-dumbbell-set-20kg.jpg", "sky"});
        model.addAttribute("picks", picks);
        model.addAttribute("pageTitle", "Home");
        return "home";
    }

    @GetMapping("/deals")
    public String deals(Model model) {
        Map<Long, Integer> offers = catalog.bannerOffers();
        List<Category> cats = catalog.categories();
        model.addAttribute("offers", offers);
        int best = 10;
        for (int v : offers.values()) best = Math.max(best, v);
        model.addAttribute("bestOff", best);
        model.addAttribute("bestDeals", catalog.topDeals(10));
        model.addAttribute("coupons", coupons.active());
        model.addAttribute("cats", cats);
        model.addAttribute("pageTitle", "Deals & Offers");
        return "deals";
    }

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("offers", catalog.bannerOffers());
        model.addAttribute("pageTitle", "All Categories");
        return "categories";
    }
}
