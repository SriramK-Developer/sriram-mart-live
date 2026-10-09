package com.srirammart.web;

import com.srirammart.config.AppUserDetails;
import com.srirammart.model.CartItem;
import com.srirammart.model.Order;
import com.srirammart.model.PaymentMethod;
import com.srirammart.model.User;
import com.srirammart.repo.UserRepository;
import com.srirammart.service.CartService;
import com.srirammart.service.CouponService;
import com.srirammart.service.OrderService;
import com.srirammart.service.UserService;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class CheckoutController {
    private static final Pattern PHONE = Pattern.compile("^[0-9]{10}$");
    private static final Pattern PINCODE = Pattern.compile("^[1-9][0-9]{5}$");
    private static final Pattern UPI = Pattern.compile("^[A-Za-z0-9._-]{2,}@[A-Za-z]{2,}$");

    private final CartService cart;
    private final CouponService coupons;
    private final OrderService orders;
    private final UserService users;
    private final UserRepository userRepo;

    public CheckoutController(CartService cart, CouponService coupons, OrderService orders, UserService users, UserRepository userRepo) {
        this.cart = cart;
        this.coupons = coupons;
        this.orders = orders;
        this.users = users;
        this.userRepo = userRepo;
    }

    @GetMapping("/checkout")
    public String form(@AuthenticationPrincipal AppUserDetails me, HttpSession session, Model model) {
        User u = users.get(me);
        List<CartItem> items = cart.items(u);
        if (items.isEmpty()) return "redirect:/cart";
        CheckoutForm f = new CheckoutForm();
        f.setFullName(u.getFullName());
        f.setPhone(u.getPhone());
        f.setAddressLine(u.getAddressLine());
        f.setCity(u.getCity());
        f.setStateName(u.getStateName());
        f.setPincode(u.getPincode() != null ? u.getPincode() : (String) session.getAttribute("pincode"));
        fill(model, u, items, session, f);
        return "checkout";
    }

    @PostMapping("/checkout")
    public String place(CheckoutForm f, @AuthenticationPrincipal AppUserDetails me, HttpSession session, Model model) {
        User u = users.get(me);
        List<CartItem> items = cart.items(u);
        if (items.isEmpty()) return "redirect:/cart";
        List<String> errors = validate(f);
        if (errors.isEmpty()) {
            try {
                Order o = orders.place(u, f, (String) session.getAttribute("coupon"));
                session.removeAttribute("coupon");
                if (u.getAddressLine() == null) {
                    u.setAddressLine(f.getAddressLine().trim());
                    u.setCity(f.getCity().trim());
                    u.setStateName(f.getStateName().trim());
                    u.setPincode(f.getPincode().trim());
                    if (u.getPhone() == null) u.setPhone(f.getPhone().trim());
                    userRepo.save(u);
                }
                return "redirect:/checkout/success/" + o.getOrderNo();
            } catch (OrderService.OrderException e) {
                errors.add(e.getMessage());
            }
        }
        model.addAttribute("errors", errors);
        fill(model, u, cart.items(u), session, f);
        return "checkout";
    }

    @GetMapping("/checkout/success/{orderNo}")
    public String success(@PathVariable String orderNo, @AuthenticationPrincipal AppUserDetails me, Model model) {
        model.addAttribute("order", orders.find(orderNo, users.get(me)));
        model.addAttribute("pageTitle", "Order placed");
        return "order-success";
    }

    private void fill(Model model, User u, List<CartItem> items, HttpSession session, CheckoutForm f) {
        model.addAttribute("form", f);
        model.addAttribute("items", items);
        model.addAttribute("summary", CartController.summarize(cart, coupons, u, session, items));
        model.addAttribute("couponCode", session.getAttribute("coupon"));
        model.addAttribute("methods", PaymentMethod.values());
        model.addAttribute("pageTitle", "Checkout");
    }

    private List<String> validate(CheckoutForm f) {
        List<String> e = new ArrayList<>();
        if (blank(f.getFullName()) || f.getFullName().trim().length() < 2) e.add("Enter the receiver's full name.");
        if (blank(f.getPhone()) || !PHONE.matcher(f.getPhone().trim()).matches()) e.add("Enter a 10-digit phone number.");
        if (blank(f.getAddressLine()) || f.getAddressLine().trim().length() < 5) e.add("Enter the delivery address (house no., street, area).");
        if (blank(f.getCity())) e.add("Enter the city.");
        if (blank(f.getStateName())) e.add("Enter the state.");
        if (blank(f.getPincode()) || !PINCODE.matcher(f.getPincode().trim()).matches()) e.add("Enter a valid 6-digit pincode.");
        String pm = f.getPaymentMethod();
        boolean known = false;
        for (PaymentMethod m : PaymentMethod.values()) if (m.name().equals(pm)) known = true;
        if (!known) e.add("Choose a payment method.");
        if ("UPI".equals(pm) && (blank(f.getUpiId()) || !UPI.matcher(f.getUpiId().trim()).matches())) e.add("Enter a valid UPI ID, for example name@bank.");
        return e;
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }
}
