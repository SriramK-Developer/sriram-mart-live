package com.srirammart.web;

import com.srirammart.config.AppUserDetails;
import com.srirammart.model.Order;
import com.srirammart.model.OrderStatus;
import com.srirammart.model.User;
import com.srirammart.service.OrderService;
import com.srirammart.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OrderController {
    private final OrderService orders;
    private final UserService users;

    public OrderController(OrderService orders, UserService users) {
        this.orders = orders;
        this.users = users;
    }

    @GetMapping("/orders")
    public String list(@AuthenticationPrincipal AppUserDetails me, Model model) {
        model.addAttribute("orders", orders.forUser(users.get(me)));
        model.addAttribute("pageTitle", "My Orders");
        return "orders";
    }

    @GetMapping("/orders/{orderNo}")
    public String detail(@PathVariable String orderNo, @AuthenticationPrincipal AppUserDetails me, Model model) {
        User u = users.get(me);
        Order o;
        try { o = orders.find(orderNo, u); } catch (OrderService.OrderException e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND); }
        model.addAttribute("order", o);
        model.addAttribute("steps", new OrderStatus[]{OrderStatus.PLACED, OrderStatus.CONFIRMED, OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED});
        model.addAttribute("pageTitle", "Order #" + o.getOrderNo());
        return "order-detail";
    }

    @PostMapping("/orders/{orderNo}/cancel")
    public String cancel(@PathVariable String orderNo, @AuthenticationPrincipal AppUserDetails me, RedirectAttributes ra) {
        try {
            orders.cancel(users.get(me), orderNo);
            ra.addFlashAttribute("flashOk", "Order cancelled. Items are back in stock.");
        } catch (OrderService.OrderException e) {
            ra.addFlashAttribute("flashError", e.getMessage());
        }
        return "redirect:/orders/" + orderNo;
    }
}
