package com.srirammart.web;

import com.srirammart.config.AppUserDetails;
import com.srirammart.model.Order;
import com.srirammart.model.OrderStatus;
import com.srirammart.model.Product;
import com.srirammart.model.Role;
import com.srirammart.model.User;
import com.srirammart.repo.ProductRepository;
import com.srirammart.repo.UserRepository;
import com.srirammart.service.CatalogService;
import com.srirammart.service.CouponService;
import com.srirammart.service.DashboardService;
import com.srirammart.service.ImageStorageService;
import com.srirammart.service.OrderService;
import com.srirammart.service.ProductService;
import com.srirammart.service.UserService;
import com.srirammart.util.CsvParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Admin console: platform overview, users, catalogue, orders, coupons and bulk import. */
@Controller
@RequestMapping("/admin")
public class AdminController {
    private static final int PAGE = 25;
    private final UserService userService;
    private final UserRepository users;
    private final ProductRepository products;
    private final DashboardService dashboard;
    private final CatalogService catalog;
    private final ProductService productService;
    private final ImageStorageService images;
    private final OrderService orders;
    private final CouponService coupons;

    public AdminController(UserService userService, UserRepository users, ProductRepository products, DashboardService dashboard,
                           CatalogService catalog, ProductService productService, ImageStorageService images,
                           OrderService orders, CouponService coupons) {
        this.userService = userService;
        this.users = users;
        this.products = products;
        this.dashboard = dashboard;
        this.catalog = catalog;
        this.productService = productService;
        this.images = images;
        this.orders = orders;
        this.coupons = coupons;
    }

    @GetMapping
    public String home(Model model) {
        model.addAllAttributes(dashboard.admin());
        model.addAttribute("pageTitle", "Admin Dashboard");
        return "admin/dashboard";
    }

    // ------------------------------------------------------------------ users
    @GetMapping("/users")
    public String users(@RequestParam(defaultValue = "ALL") String role, Model model) {
        List<User> list;
        try { list = "ALL".equals(role) ? users.findAllByOrderByCreatedAtDesc() : users.findByRoleOrderByCreatedAtDesc(Role.valueOf(role)); }
        catch (IllegalArgumentException e) { list = users.findAllByOrderByCreatedAtDesc(); role = "ALL"; }
        model.addAttribute("list", list);
        model.addAttribute("role", role);
        model.addAttribute("pageTitle", "Users");
        return "admin/users";
    }

    @PostMapping("/users/{id}/enabled")
    public String enable(@PathVariable Long id, @RequestParam boolean enabled, RedirectAttributes ra) {
        userService.setEnabled(id, enabled);
        ra.addFlashAttribute("flashOk", enabled ? "Account enabled." : "Account disabled.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes ra) {
        userService.approveSeller(id);
        ra.addFlashAttribute("flashOk", "Seller approved. They can sign in now.");
        return "redirect:/admin/users?role=SELLER";
    }

    // ------------------------------------------------------------------ products
    @GetMapping("/products")
    public String products(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page, Model model) {
        String term = q.trim().length() > 60 ? q.trim().substring(0, 60) : q.trim();
        Page<Product> pg = products.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCaseOrderByIdDesc(term, term, PageRequest.of(Math.max(0, page), PAGE));
        model.addAttribute("items", pg.getContent());
        model.addAttribute("pg", pg);
        model.addAttribute("q", term);
        model.addAttribute("pageTitle", "Products");
        return "admin/products";
    }

    @PostMapping("/products/{id}/toggle")
    public String toggle(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me, @RequestParam(defaultValue = "") String q) {
        productService.toggleActive(id, userService.get(me));
        return "redirect:/admin/products" + (q.isBlank() ? "" : "?q=" + java.net.URLEncoder.encode(q, StandardCharsets.UTF_8));
    }

    @PostMapping("/products/{id}/stock")
    public String stock(@PathVariable Long id, @RequestParam int stock, @AuthenticationPrincipal AppUserDetails me, RedirectAttributes ra) {
        productService.setStock(id, stock, userService.get(me));
        ra.addFlashAttribute("flashOk", "Stock updated.");
        return "redirect:/admin/products";
    }

    @GetMapping("/products/new")
    public String newProduct(Model model) {
        ProductForm f = new ProductForm();
        f.setStock(10);
        return form(model, f, "Add product", null);
    }

    @GetMapping("/products/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Product p = catalog.product(id);
        if (p == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return form(model, productService.toForm(p), "Edit product", p);
    }

    private String form(Model model, ProductForm f, String title, Product p) {
        model.addAttribute("form", f);
        model.addAttribute("product", p);
        model.addAttribute("formTitle", title);
        model.addAttribute("formAction", "/admin/products/save");
        model.addAttribute("backUrl", "/admin/products");
        model.addAttribute("cats", catalog.categories());
        model.addAttribute("sellers", users.findByRoleOrderByCreatedAtDesc(Role.SELLER));
        model.addAttribute("pageTitle", title);
        return "seller/product-form";
    }

    @PostMapping("/products/save")
    public String save(@ModelAttribute ProductForm form, @RequestParam(required = false) MultipartFile image,
                       @RequestParam(required = false) Long sellerId, @AuthenticationPrincipal AppUserDetails me,
                       Model model, RedirectAttributes ra) {
        List<String> errors = new ArrayList<>();
        String path = null;
        try { path = images.save(image); }
        catch (IllegalArgumentException e) { errors.add(e.getMessage()); }
        catch (IOException e) { errors.add("Could not store the image. Try again."); }
        if (errors.isEmpty()) {
            User owner = sellerId == null ? null : users.findById(sellerId).filter(u -> u.getRole() == Role.SELLER).orElse(null);
            errors.addAll(productService.save(form, path, userService.get(me), owner, new Product[1]));
        }
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            return form(model, form, form.getId() == null ? "Add product" : "Edit product", form.getId() == null ? null : catalog.product(form.getId()));
        }
        ra.addFlashAttribute("flashOk", "Product saved.");
        return "redirect:/admin/products";
    }

    // ------------------------------------------------------------------ orders
    @GetMapping("/orders")
    public String orders(Model model) {
        model.addAttribute("list", orders.recent(100));
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("pageTitle", "Orders");
        return "admin/orders";
    }

    @PostMapping("/orders/{orderNo}/status")
    public String status(@PathVariable String orderNo, @RequestParam String status, RedirectAttributes ra) {
        try {
            orders.updateOrderStatus(orderNo, OrderStatus.valueOf(status));
            ra.addFlashAttribute("flashOk", "Order " + orderNo + " updated.");
        } catch (OrderService.OrderException e) {
            ra.addFlashAttribute("flashError", e.getMessage());
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("flashError", "Unknown status.");
        }
        return "redirect:/admin/orders";
    }

    // ------------------------------------------------------------------ coupons
    @GetMapping("/coupons")
    public String coupons(Model model) {
        model.addAttribute("list", coupons.all());
        model.addAttribute("pageTitle", "Coupons");
        return "admin/coupons";
    }

    @PostMapping("/coupons")
    public String createCoupon(@RequestParam String code, @RequestParam(defaultValue = "") String description,
                               @RequestParam(defaultValue = "0") int percent, @RequestParam(required = false) String flat,
                               @RequestParam(required = false) String max, @RequestParam(required = false) String min,
                               @RequestParam(required = false) String expires, RedirectAttributes ra) {
        LocalDate exp = null;
        try { if (expires != null && !expires.isBlank()) exp = LocalDate.parse(expires); }
        catch (Exception e) { ra.addFlashAttribute("flashError", "Enter a valid expiry date."); return "redirect:/admin/coupons"; }
        String err = coupons.create(code, description.trim(), percent, num(flat), num(max), num(min), exp);
        if (err != null) ra.addFlashAttribute("flashError", err); else ra.addFlashAttribute("flashOk", "Coupon created.");
        return "redirect:/admin/coupons";
    }

    @PostMapping("/coupons/{id}/toggle")
    public String toggleCoupon(@PathVariable Long id) {
        coupons.toggle(id);
        return "redirect:/admin/coupons";
    }

    private static BigDecimal num(String s) {
        try { return s == null || s.isBlank() ? null : new BigDecimal(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    // ------------------------------------------------------------------ bulk import
    @GetMapping("/import")
    public String importPage(Model model) {
        model.addAttribute("pageTitle", "Bulk import");
        return "admin/import";
    }

    @PostMapping("/import")
    public String doImport(@RequestParam MultipartFile file, Model model) {
        model.addAttribute("pageTitle", "Bulk import");
        if (file == null || file.isEmpty()) {
            model.addAttribute("importError", "Choose a CSV file first.");
            return "admin/import";
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            model.addAttribute("importError", "File is larger than 10 MB.");
            return "admin/import";
        }
        try (var reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            List<Map<String, String>> rows = CsvParser.readMaps(reader);
            if (rows.size() > 20000) { model.addAttribute("importError", "Import at most 20,000 rows at a time."); return "admin/import"; }
            model.addAttribute("result", productService.importRows(rows));
        } catch (IOException e) {
            model.addAttribute("importError", "Could not read the file.");
        }
        return "admin/import";
    }
}
