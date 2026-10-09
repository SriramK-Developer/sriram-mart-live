package com.srirammart.service;

import com.srirammart.model.CartItem;
import com.srirammart.model.NotificationType;
import com.srirammart.model.Order;
import com.srirammart.model.OrderItem;
import com.srirammart.model.OrderStatus;
import com.srirammart.model.PaymentMethod;
import com.srirammart.model.PaymentStatus;
import com.srirammart.model.Product;
import com.srirammart.model.Role;
import com.srirammart.model.User;
import com.srirammart.repo.CartItemRepository;
import com.srirammart.repo.OrderItemRepository;
import com.srirammart.repo.OrderRepository;
import com.srirammart.repo.ProductRepository;
import com.srirammart.web.CheckoutForm;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    /** Business rule violation with a message that is safe to show to the user. */
    public static class OrderException extends RuntimeException {
        public OrderException(String message) { super(message); }
    }

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final CartItemRepository carts;
    private final ProductRepository products;
    private final CouponService coupons;
    private final CartService cartService;
    private final NotificationService notifications;

    public OrderService(OrderRepository orders, OrderItemRepository orderItems, CartItemRepository carts,
                        ProductRepository products, CouponService coupons, CartService cartService,
                        NotificationService notifications) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.carts = carts;
        this.products = products;
        this.coupons = coupons;
        this.cartService = cartService;
        this.notifications = notifications;
    }

    /** Delivery estimate from the pincode region (simulated courier zones). */
    public static int etaDays(String pincode) {
        if (pincode == null || pincode.isBlank()) return 3;
        char c = pincode.trim().charAt(0);
        if (c == '6') return 2;
        if (c == '5' || c == '7') return 3;
        return 4;
    }

    public Order find(String orderNo, User user) {
        return orders.findByOrderNoAndUser(orderNo, user).orElseThrow(() -> new OrderException("Order not found."));
    }
    public Order findAny(String orderNo) {
        return orders.findByOrderNo(orderNo).orElseThrow(() -> new OrderException("Order not found."));
    }
    public List<Order> forUser(User u) { return orders.findByUserOrderByCreatedAtDesc(u); }
    public long count(User u) { return orders.countByUser(u); }

    /**
     * Turns the cart into an order. Stock is reduced with a conditional UPDATE inside the same transaction,
     * so two shoppers can never buy the last unit twice; any failure rolls the whole order back.
     */
    @Transactional
    public Order place(User user, CheckoutForm f, String couponCode) {
        List<CartItem> cart = carts.findByUserOrderByIdAsc(user);
        if (cart.isEmpty()) throw new OrderException("Your cart is empty.");
        List<CartItem> sorted = new ArrayList<>(cart);
        sorted.sort(Comparator.comparing(c -> c.getProduct().getId()));

        BigDecimal priceTotal = BigDecimal.ZERO, mrpTotal = BigDecimal.ZERO;
        for (CartItem ci : sorted) {
            Product p = ci.getProduct();
            if (!p.isActive()) throw new OrderException(p.getName() + " is no longer available. Remove it from your cart.");
            if (products.decrementStock(p.getId(), ci.getQuantity()) == 0) {
                Product fresh = products.findById(p.getId()).orElse(p);
                throw new OrderException(fresh.getStock() <= 0 ? p.getName() + " just went out of stock."
                        : "Only " + fresh.getStock() + " unit(s) of " + p.getName() + " left. Reduce the quantity and try again.");
            }
            priceTotal = priceTotal.add(ci.getLineTotal());
            mrpTotal = mrpTotal.add(ci.getLineMrp());
        }

        BigDecimal couponDiscount = BigDecimal.ZERO;
        String appliedCode = null;
        if (couponCode != null && !couponCode.isBlank()) {
            CouponService.Result r = coupons.evaluate(couponCode, priceTotal);
            if (r.isValid()) { couponDiscount = r.getDiscount(); appliedCode = r.getCoupon().getCode(); }
        }
        BigDecimal delivery = priceTotal.compareTo(CartService.FREE_DELIVERY_MIN) >= 0 ? BigDecimal.ZERO : CartService.DELIVERY_FEE;

        PaymentMethod pm;
        try { pm = PaymentMethod.valueOf(f.getPaymentMethod()); } catch (Exception e) { pm = PaymentMethod.COD; }

        Order o = new Order();
        o.setOrderNo("TMP" + System.nanoTime());
        o.setUser(user);
        o.setPaymentMethod(pm);
        o.setPaymentStatus(pm == PaymentMethod.COD ? PaymentStatus.PENDING : PaymentStatus.PAID);
        o.setSubtotal(mrpTotal);
        o.setDiscount(mrpTotal.subtract(priceTotal).add(couponDiscount));
        o.setDeliveryCharge(delivery);
        o.setTotal(priceTotal.subtract(couponDiscount).add(delivery));
        o.setCouponCode(appliedCode);
        o.setShipName(f.getFullName().trim());
        o.setShipPhone(f.getPhone().trim());
        o.setShipAddress(f.getAddressLine().trim());
        o.setShipCity(f.getCity().trim());
        o.setShipState(f.getStateName().trim());
        o.setShipPincode(f.getPincode().trim());
        o.setExpectedDelivery(LocalDate.now().plusDays(etaDays(f.getPincode())));
        for (CartItem ci : sorted) {
            Product p = ci.getProduct();
            OrderItem oi = new OrderItem();
            oi.setProduct(p);
            oi.setSeller(p.getSeller());
            oi.setProductName(p.getName());
            oi.setSubtitle(p.getSubtitle());
            oi.setImagePath(p.getImageMain());
            oi.setUnitPrice(p.getPrice());
            oi.setMrp(p.getMrp());
            oi.setQuantity(ci.getQuantity());
            oi.setLineTotal(ci.getLineTotal());
            o.addItem(oi);
        }
        o = orders.save(o);
        o.setOrderNo(nextOrderNo(o.getId()));
        o = orders.save(o);
        carts.deleteAllByUser(user);
        if (appliedCode != null) coupons.markUsed(appliedCode);

        notifications.push(user, NotificationType.ORDER, "Order confirmed",
                "Your order #" + o.getOrderNo() + " has been placed. Estimated delivery: " + o.getExpectedDelivery() + ".",
                "/orders/" + o.getOrderNo(), "View Order");
        Set<User> sellers = new LinkedHashSet<>();
        for (OrderItem i : o.getItems()) sellers.add(i.getSeller());
        for (User s : sellers) {
            notifications.push(s, NotificationType.ORDER, "New order received",
                    "Order #" + o.getOrderNo() + " contains items from your store.", "/seller/orders", "View orders");
        }
        return o;
    }

    /** Readable order numbers such as SM123456, skipping any number already in use. */
    public String nextOrderNo(Long id) {
        long n = 123000 + (id == null ? 0 : id);
        String no;
        do { no = "SM" + n++; } while (orders.existsByOrderNo(no));
        return no;
    }

    /** Buyer cancellation, only before the order ships. Stock goes back on the shelf. */
    @Transactional
    public void cancel(User user, String orderNo) {
        Order o = find(orderNo, user);
        if (!o.isCancellable()) throw new OrderException("This order has already shipped, so it can't be cancelled. You can return it after delivery.");
        for (OrderItem i : o.getItems()) cancelItem(i);
        if (o.getPaymentStatus() == PaymentStatus.PAID) o.setPaymentStatus(PaymentStatus.REFUNDED);
        o.recomputeStatus();
        orders.save(o);
        notifications.push(user, NotificationType.ORDER, "Order cancelled",
                "Your order #" + o.getOrderNo() + " was cancelled." + (o.getPaymentStatus() == PaymentStatus.REFUNDED ? " Your refund is on its way." : ""),
                "/orders/" + o.getOrderNo(), "View Order");
    }

    private void cancelItem(OrderItem i) {
        if (i.getStatus() == OrderStatus.CANCELLED) return;
        i.setStatus(OrderStatus.CANCELLED);
        products.restoreStock(i.getProduct().getId(), i.getQuantity());
    }

    /** Seller (own items only) or admin moves an item forward, or cancels it. */
    @Transactional
    public void updateItemStatus(Long itemId, OrderStatus next, User actor) {
        OrderItem i = orderItems.findById(itemId).orElseThrow(() -> new OrderException("Order item not found."));
        if (actor.getRole() == Role.SELLER && !i.getSeller().getId().equals(actor.getId()))
            throw new OrderException("You can only update orders for your own products.");
        applyStatus(i, next);
        finishUpdate(i.getOrder());
    }

    /** Admin: set the same status on every open item of an order. */
    @Transactional
    public void updateOrderStatus(String orderNo, OrderStatus next) {
        Order o = findAny(orderNo);
        for (OrderItem i : o.getItems()) {
            if (!i.getStatus().isFinal() && (next == OrderStatus.CANCELLED || next.ordinal() > i.getStatus().ordinal())) applyStatus(i, next);
        }
        finishUpdate(o);
    }

    private void applyStatus(OrderItem i, OrderStatus next) {
        OrderStatus cur = i.getStatus();
        if (cur.isFinal()) throw new OrderException("This item is already " + cur.getLabel().toLowerCase() + ".");
        if (next == OrderStatus.CANCELLED) { cancelItem(i); return; }
        if (next.ordinal() <= cur.ordinal()) throw new OrderException("Status can only move forward.");
        i.setStatus(next);
    }

    private void finishUpdate(Order o) {
        o.recomputeStatus();
        boolean allDone = true, anyLive = false;
        for (OrderItem i : o.getItems()) {
            if (i.getStatus() == OrderStatus.CANCELLED) continue;
            anyLive = true;
            if (i.getStatus() != OrderStatus.DELIVERED) allDone = false;
        }
        if (anyLive && allDone && o.getPaymentMethod() == PaymentMethod.COD) o.setPaymentStatus(PaymentStatus.PAID);
        if (!anyLive && o.getPaymentStatus() == PaymentStatus.PAID) o.setPaymentStatus(PaymentStatus.REFUNDED);
        orders.save(o);
        notifications.push(o.getUser(), NotificationType.ORDER,
                o.getStatus() == OrderStatus.DELIVERED ? "Order delivered" : o.getStatus() == OrderStatus.SHIPPED ? "Order update" : "Order status updated",
                "Your order #" + o.getOrderNo() + " is now: " + o.getStatus().getLabel() + ".",
                "/orders/" + o.getOrderNo(), "View Order");
    }

    public List<Order> forSeller(User seller) { return orders.findBySeller(seller); }
    public List<Order> recent(int n) { return orders.findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, n)); }
    public long countCartLines(User u) { return cartService.count(u); }
}
