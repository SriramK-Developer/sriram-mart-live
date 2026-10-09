package com.srirammart.web;

import com.srirammart.config.AppUserDetails;
import com.srirammart.model.Product;
import com.srirammart.model.Role;
import com.srirammart.model.User;
import com.srirammart.service.CatalogService;
import com.srirammart.service.CouponService;
import com.srirammart.service.OrderService;
import com.srirammart.service.ReviewService;
import com.srirammart.service.UserService;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProductController {
    private final CatalogService catalog;
    private final ReviewService reviews;
    private final UserService users;
    private final CouponService coupons;

    public ProductController(CatalogService catalog, ReviewService reviews, UserService users, CouponService coupons) {
        this.coupons = coupons;
        this.catalog = catalog;
        this.reviews = reviews;
        this.users = users;
    }

    @GetMapping("/product/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me, HttpSession session, Model model) {
        Product p = catalog.product(id);
        User u = me == null ? null : users.get(me);
        if (p == null || (!p.isActive() && (u == null || u.getRole() == Role.BUYER))) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        String pin = (String) session.getAttribute("pincode");
        if (pin == null || pin.isBlank()) pin = u != null && u.getPincode() != null ? u.getPincode() : "600001";
        int days = OrderService.etaDays(pin);
        model.addAttribute("product", p);
        model.addAttribute("reviews", reviews.latest(p, 6));
        model.addAttribute("dist", reviews.distribution(p));
        model.addAttribute("canReview", u != null && u.getRole() == Role.BUYER && !reviews.hasReviewed(p, u));
        model.addAttribute("related", catalog.related(p, 5));
        model.addAttribute("pincode", pin);
        model.addAttribute("coupons", coupons.active());
        model.addAttribute("eta", LocalDate.now().plusDays(days));
        model.addAttribute("freeDelivery", p.getPrice().compareTo(com.srirammart.service.CartService.FREE_DELIVERY_MIN) >= 0);
        model.addAttribute("pageTitle", p.getName());
        return "product";
    }

    @PostMapping("/product/{id}/review")
    public String review(@PathVariable Long id, @RequestParam int stars, @RequestParam String body,
                         @AuthenticationPrincipal AppUserDetails me, RedirectAttributes ra) {
        if (me.getRole() != Role.BUYER) return "redirect:/product/" + id;
        String err = reviews.add(users.get(me), id, stars, body);
        if (err != null) ra.addFlashAttribute("flashError", err);
        else ra.addFlashAttribute("flashOk", "Thanks! Your review has been posted.");
        return "redirect:/product/" + id + "#reviews";
    }

    @PostMapping("/product/{id}/pincode")
    public String pincode(@PathVariable Long id, @RequestParam String pincode, HttpSession session, RedirectAttributes ra) {
        if (pincode != null && pincode.trim().matches("[1-9][0-9]{5}")) session.setAttribute("pincode", pincode.trim());
        else ra.addFlashAttribute("flashError", "Enter a valid 6-digit pincode.");
        return "redirect:/product/" + id;
    }
}
