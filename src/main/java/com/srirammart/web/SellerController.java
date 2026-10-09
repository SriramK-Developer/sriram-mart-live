package com.srirammart.web;

import com.srirammart.config.AppUserDetails;
import com.srirammart.model.Order;
import com.srirammart.model.OrderItem;
import com.srirammart.model.OrderStatus;
import com.srirammart.model.Product;
import com.srirammart.model.User;
import com.srirammart.repo.OrderItemRepository;
import com.srirammart.service.CatalogService;
import com.srirammart.service.DashboardService;
import com.srirammart.service.ImageStorageService;
import com.srirammart.service.OrderService;
import com.srirammart.service.ProductService;
import com.srirammart.service.UserService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

/** Seller console: sales overview, own products and stock, orders to fulfil. */
@Controller
@RequestMapping("/seller")
public class SellerController {
    private final UserService users;
    private final DashboardService dashboard;
    private final CatalogService catalog;
    private final ProductService productService;
    private final ImageStorageService images;
    private final OrderService orders;
    private final OrderItemRepository orderItems;

    public SellerController(UserService users, DashboardService dashboard, CatalogService catalog, ProductService productService,
                            ImageStorageService images, OrderService orders, OrderItemRepository orderItems) {
        this.users = users;
        this.dashboard = dashboard;
        this.catalog = catalog;
        this.productService = productService;
        this.images = images;
        this.orders = orders;
        this.orderItems = orderItems;
    }

    @GetMapping
    public String home(@AuthenticationPrincipal AppUserDetails me, Model model) {
        User s = users.get(me);
        model.addAllAttributes(dashboard.seller(s));
        model.addAttribute("pageTitle", "Seller Dashboard");
        return "seller/dashboard";
    }

    @GetMapping("/products")
    public String products(@AuthenticationPrincipal AppUserDetails me, Model model) {
        model.addAttribute("items", catalog.sellerProducts(users.get(me)));
        model.addAttribute("pageTitle", "My Products");
        return "seller/products";
    }

    @GetMapping("/products/new")
    public String newProduct(Model model) {
        ProductForm f = new ProductForm();
        f.setStock(10);
        return form(model, f, "Add product");
    }

    @GetMapping("/products/{id}/edit")
    public String edit(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me, Model model) {
        Product p = catalog.product(id);
        if (p == null || !p.getSeller().getId().equals(me.getId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        model.addAttribute("product", p);
        return form(model, productService.toForm(p), "Edit product");
    }

    private String form(Model model, ProductForm f, String title) {
        model.addAttribute("form", f);
        model.addAttribute("formTitle", title);
        model.addAttribute("formAction", "/seller/products/save");
        model.addAttribute("backUrl", "/seller/products");
        model.addAttribute("cats", catalog.categories());
        model.addAttribute("pageTitle", title);
        return "seller/product-form";
    }

    @PostMapping("/products/save")
    public String save(@ModelAttribute ProductForm form, @RequestParam(required = false) MultipartFile image,
                       @AuthenticationPrincipal AppUserDetails me, Model model, RedirectAttributes ra) {
        User s = users.get(me);
        List<String> errors = new ArrayList<>();
        String path = null;
        try { path = images.save(image); }
        catch (IllegalArgumentException e) { errors.add(e.getMessage()); }
        catch (IOException e) { errors.add("Could not store the image. Try again."); }
        if (errors.isEmpty()) {
            Product[] out = new Product[1];
            errors.addAll(productService.save(form, path, s, null, out));
        }
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            if (form.getId() != null) model.addAttribute("product", catalog.product(form.getId()));
            return form(model, form, form.getId() == null ? "Add product" : "Edit product");
        }
        ra.addFlashAttribute("flashOk", "Product saved.");
        return "redirect:/seller/products";
    }

    @PostMapping("/products/{id}/stock")
    public String stock(@PathVariable Long id, @RequestParam int stock, @AuthenticationPrincipal AppUserDetails me, RedirectAttributes ra) {
        productService.setStock(id, stock, users.get(me));
        ra.addFlashAttribute("flashOk", "Stock updated.");
        return "redirect:/seller/products";
    }

    @PostMapping("/products/{id}/toggle")
    public String toggle(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails me) {
        productService.toggleActive(id, users.get(me));
        return "redirect:/seller/products";
    }

    @GetMapping("/orders")
    public String orders(@AuthenticationPrincipal AppUserDetails me, Model model) {
        User s = users.get(me);
        model.addAttribute("items", orderItems.findBySellerOrderByIdDesc(s));
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("pageTitle", "Orders");
        return "seller/orders";
    }

    @PostMapping("/orders/items/{itemId}/status")
    public String status(@PathVariable Long itemId, @RequestParam String status, @AuthenticationPrincipal AppUserDetails me, RedirectAttributes ra) {
        try {
            orders.updateItemStatus(itemId, OrderStatus.valueOf(status), users.get(me));
            ra.addFlashAttribute("flashOk", "Order status updated. The buyer has been notified.");
        } catch (OrderService.OrderException | IllegalArgumentException e) {
            ra.addFlashAttribute("flashError", e instanceof OrderService.OrderException ? e.getMessage() : "Unknown status.");
        }
        return "redirect:/seller/orders";
    }
}
