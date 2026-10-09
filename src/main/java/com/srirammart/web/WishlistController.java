package com.srirammart.web;

import com.srirammart.config.AppUserDetails;
import com.srirammart.model.User;
import com.srirammart.service.UserService;
import com.srirammart.service.WishlistService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class WishlistController {
    private final WishlistService wishlist;
    private final UserService users;

    public WishlistController(WishlistService wishlist, UserService users) {
        this.wishlist = wishlist;
        this.users = users;
    }

    @GetMapping("/wishlist")
    public String view(@AuthenticationPrincipal AppUserDetails me, Model model) {
        User u = users.get(me);
        var items = wishlist.items(u);
        BigDecimal total = BigDecimal.ZERO;
        for (var w : items) total = total.add(w.getProduct().getPrice());
        model.addAttribute("items", items);
        model.addAttribute("total", total);
        model.addAttribute("pageTitle", "My Wishlist");
        return "wishlist";
    }

    @PostMapping("/wishlist/clear")
    public String clear(@AuthenticationPrincipal AppUserDetails me) {
        wishlist.clear(users.get(me));
        return "redirect:/wishlist";
    }
}
