package com.srirammart.web;

import com.srirammart.config.AppUserDetails;
import com.srirammart.model.User;
import com.srirammart.service.CartService;
import com.srirammart.service.CatalogService;
import com.srirammart.service.NotificationService;
import com.srirammart.service.UserService;
import com.srirammart.service.WishlistService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Values every page needs: the signed-in user, badge counts and the category list for the sidebar. */
@ControllerAdvice
public class GlobalModelAdvice {
    private final UserService users;
    private final CatalogService catalog;
    private final CartService cart;
    private final WishlistService wishlist;
    private final NotificationService notifications;

    public GlobalModelAdvice(UserService users, CatalogService catalog, CartService cart,
                             WishlistService wishlist, NotificationService notifications) {
        this.users = users;
        this.catalog = catalog;
        this.cart = cart;
        this.wishlist = wishlist;
        this.notifications = notifications;
    }

    @ModelAttribute
    public void globals(Model model, @AuthenticationPrincipal AppUserDetails principal, HttpServletRequest req) {
        String uri = req.getRequestURI();
        if (uri.startsWith("/api/")) return;
        model.addAttribute("uri", uri);
        model.addAttribute("categoryList", catalog.categories());
        if (principal != null) {
            User u = users.get(principal);
            model.addAttribute("me", u);
            model.addAttribute("unread", notifications.unread(u));
            if (u.getRole() == com.srirammart.model.Role.BUYER) {
                model.addAttribute("cartCount", cart.count(u));
                model.addAttribute("wishCount", wishlist.count(u));
                model.addAttribute("wishIds", wishlist.ids(u));
            }
        }
    }
}
