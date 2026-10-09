package com.srirammart.web;

import com.srirammart.config.AppUserDetails;
import com.srirammart.model.CartItem;
import com.srirammart.model.User;
import com.srirammart.service.CartService;
import com.srirammart.service.CouponService;
import com.srirammart.service.UserService;
import com.srirammart.service.WishlistService;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CartController {
    private static final String COUPON = "coupon";
    private final CartService cart;
    private final CouponService coupons;
    private final UserService users;
    private final WishlistService wishlist;

    public CartController(CartService cart, CouponService coupons, UserService users, WishlistService wishlist) {
        this.cart = cart;
        this.coupons = coupons;
        this.users = users;
        this.wishlist = wishlist;
    }

    /** Applies the session coupon (if any) to the current cart and returns the resulting summary. */
    static CartService.Summary summarize(CartService cart, CouponService coupons, User u, HttpSession session, List<CartItem> items) {
        BigDecimal discount = BigDecimal.ZERO;
        String code = (String) session.getAttribute(COUPON);
        if (code != null) {
            BigDecimal value = cart.summary(items, BigDecimal.ZERO).getPriceTotal();
            CouponService.Result r = coupons.evaluate(code, value);
            if (r.isValid()) discount = r.getDiscount(); else session.removeAttribute(COUPON);
        }
        return cart.summary(items, discount);
    }

    @GetMapping("/cart")
    public String view(@AuthenticationPrincipal AppUserDetails me, HttpSession session, Model model) {
        User u = users.get(me);
        List<CartItem> items = cart.items(u);
        model.addAttribute("items", items);
        model.addAttribute("summary", summarize(cart, coupons, u, session, items));
        model.addAttribute("couponCode", session.getAttribute(COUPON));
        model.addAttribute("coupons", coupons.active());
        model.addAttribute("maxQty", CartService.MAX_QTY);
        model.addAttribute("pageTitle", "My Cart");
        return "cart";
    }

    /** AJAX add-to-cart used by product cards and the product page. */
    @PostMapping("/api/cart/add")
    @ResponseBody
    public Map<String, Object> apiAdd(@RequestParam Long productId, @RequestParam(defaultValue = "1") int qty,
                                      @AuthenticationPrincipal AppUserDetails me) {
        Map<String, Object> out = new LinkedHashMap<>();
        User u = users.get(me);
        try {
            String note = cart.add(u, productId, Math.min(qty, CartService.MAX_QTY));
            out.put("ok", true);
            out.put("message", note != null ? note : "Added to cart");
        } catch (IllegalStateException e) {
            out.put("ok", false);
            out.put("message", e.getMessage());
        }
        out.put("count", cart.count(u));
        return out;
    }

    @PostMapping("/api/wishlist/toggle")
    @ResponseBody
    public Map<String, Object> toggleWish(@RequestParam Long productId, @AuthenticationPrincipal AppUserDetails me) {
        User u = users.get(me);
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            boolean now = wishlist.toggle(u, productId);
            out.put("ok", true);
            out.put("wished", now);
            out.put("message", now ? "Added to wishlist" : "Removed from wishlist");
        } catch (RuntimeException e) {
            out.put("ok", false);
            out.put("message", "Product not found.");
        }
        out.put("count", wishlist.count(u));
        return out;
    }

    @PostMapping("/cart/buy-now")
    public String buyNow(@RequestParam Long productId, @RequestParam(defaultValue = "1") int qty,
                         @AuthenticationPrincipal AppUserDetails me, RedirectAttributes ra) {
        try {
            String note = cart.add(users.get(me), productId, Math.min(qty, CartService.MAX_QTY));
            if (note != null) ra.addFlashAttribute("flashOk", note);
            return "redirect:/checkout";
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("flashError", e.getMessage());
            return "redirect:/product/" + productId;
        }
    }

    @PostMapping("/cart/update")
    public String update(@RequestParam Long itemId, @RequestParam int qty, @AuthenticationPrincipal AppUserDetails me, RedirectAttributes ra) {
        String note = cart.update(users.get(me), itemId, qty);
        if (note != null) ra.addFlashAttribute("flashError", note);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String remove(@RequestParam Long itemId, @AuthenticationPrincipal AppUserDetails me) {
        cart.remove(users.get(me), itemId);
        return "redirect:/cart";
    }

    @PostMapping("/cart/clear")
    public String clear(@AuthenticationPrincipal AppUserDetails me, HttpSession session) {
        cart.clear(users.get(me));
        session.removeAttribute(COUPON);
        return "redirect:/cart";
    }

    @PostMapping("/cart/move-to-wishlist")
    public String moveAll(@AuthenticationPrincipal AppUserDetails me, HttpSession session, RedirectAttributes ra) {
        User u = users.get(me);
        var already = wishlist.ids(u);
        int moved = 0;
        for (CartItem c : cart.items(u)) {
            if (!already.contains(c.getProduct().getId())) { wishlist.toggle(u, c.getProduct().getId()); moved++; }
        }
        cart.clear(u);
        session.removeAttribute(COUPON);
        ra.addFlashAttribute("flashOk", "Moved " + moved + " item(s) to your wishlist.");
        return "redirect:/wishlist";
    }

    @PostMapping("/cart/coupon")
    public String coupon(@RequestParam(required = false) String code, @RequestParam(defaultValue = "apply") String action,
                         @AuthenticationPrincipal AppUserDetails me, HttpSession session, RedirectAttributes ra) {
        if ("remove".equals(action)) { session.removeAttribute(COUPON); return "redirect:/cart"; }
        User u = users.get(me);
        BigDecimal value = cart.summary(cart.items(u), BigDecimal.ZERO).getPriceTotal();
        CouponService.Result r = coupons.evaluate(code, value);
        if (r.isValid()) {
            session.setAttribute(COUPON, r.getCoupon().getCode());
            ra.addFlashAttribute("flashOk", "Coupon " + r.getCoupon().getCode() + " applied — you save ₹" + r.getDiscount().setScale(0, java.math.RoundingMode.HALF_UP) + ".");
        } else ra.addFlashAttribute("flashError", r.getError());
        return "redirect:/cart";
    }
}
