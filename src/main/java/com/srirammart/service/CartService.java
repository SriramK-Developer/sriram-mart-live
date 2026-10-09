package com.srirammart.service;

import com.srirammart.model.CartItem;
import com.srirammart.model.Product;
import com.srirammart.model.User;
import com.srirammart.repo.CartItemRepository;
import com.srirammart.repo.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {
    public static final int MAX_QTY = 10;
    public static final BigDecimal FREE_DELIVERY_MIN = BigDecimal.valueOf(499);
    public static final BigDecimal DELIVERY_FEE = BigDecimal.valueOf(40);

    private final CartItemRepository carts;
    private final ProductRepository products;

    public CartService(CartItemRepository carts, ProductRepository products) {
        this.carts = carts;
        this.products = products;
    }

    public static class Summary {
        private final int lines;
        private final int units;
        private final BigDecimal mrpTotal;
        private final BigDecimal priceTotal;
        private final BigDecimal couponDiscount;
        private final BigDecimal delivery;
        private final BigDecimal total;
        Summary(int lines, int units, BigDecimal mrpTotal, BigDecimal priceTotal, BigDecimal couponDiscount) {
            this.lines = lines;
            this.units = units;
            this.mrpTotal = mrpTotal;
            this.priceTotal = priceTotal;
            this.couponDiscount = couponDiscount;
            this.delivery = lines == 0 || priceTotal.compareTo(FREE_DELIVERY_MIN) >= 0 ? BigDecimal.ZERO : DELIVERY_FEE;
            this.total = priceTotal.subtract(couponDiscount).add(delivery);
        }
        public int getLines() { return lines; }
        public int getUnits() { return units; }
        public BigDecimal getMrpTotal() { return mrpTotal; }
        public BigDecimal getPriceTotal() { return priceTotal; }
        public BigDecimal getProductDiscount() { return mrpTotal.subtract(priceTotal); }
        public BigDecimal getCouponDiscount() { return couponDiscount; }
        public BigDecimal getTotalDiscount() { return getProductDiscount().add(couponDiscount); }
        public BigDecimal getDelivery() { return delivery; }
        public boolean isFreeDelivery() { return delivery.signum() == 0; }
        public BigDecimal getTotal() { return total; }
    }

    public List<CartItem> items(User u) { return carts.findByUserOrderByIdAsc(u); }
    public long count(User u) { return carts.countByUser(u); }

    public Summary summary(List<CartItem> items, BigDecimal couponDiscount) {
        BigDecimal mrp = BigDecimal.ZERO, price = BigDecimal.ZERO;
        int units = 0;
        for (CartItem c : items) { mrp = mrp.add(c.getLineMrp()); price = price.add(c.getLineTotal()); units += c.getQuantity(); }
        BigDecimal cd = couponDiscount == null ? BigDecimal.ZERO : couponDiscount.min(price);
        return new Summary(items.size(), units, mrp, price, cd);
    }

    /**
     * Adds a product to the cart.
     * @return null, or a note when the quantity had to be adjusted to the available stock
     * @throws IllegalStateException when the product cannot be bought (message is safe to show)
     */
    @Transactional
    public String add(User u, Long productId, int qty) {
        Product p = products.findById(productId).orElse(null);
        if (p == null || !p.isActive()) throw new IllegalStateException("This product is no longer available.");
        if (p.getStock() <= 0) throw new IllegalStateException(p.getName() + " is out of stock.");
        qty = Math.max(1, qty);
        CartItem item = carts.findByUserAndProduct(u, p).orElse(null);
        int want = (item == null ? 0 : item.getQuantity()) + qty;
        int cap = Math.min(MAX_QTY, p.getStock());
        String note = null;
        if (want > cap) { want = cap; note = "Only " + cap + " available for " + p.getName() + ". Quantity adjusted."; }
        if (item == null) { item = new CartItem(); item.setUser(u); item.setProduct(p); }
        item.setQuantity(want);
        carts.save(item);
        return note;
    }

    @Transactional
    public String update(User u, Long itemId, int qty) {
        CartItem item = carts.findByIdAndUser(itemId, u).orElse(null);
        if (item == null) return null;
        if (qty <= 0) { carts.delete(item); return null; }
        int cap = Math.min(MAX_QTY, Math.max(1, item.getProduct().getStock()));
        String note = null;
        if (qty > cap) { qty = cap; note = "Only " + cap + " available for " + item.getProduct().getName() + "."; }
        item.setQuantity(qty);
        carts.save(item);
        return note;
    }

    @Transactional
    public void remove(User u, Long itemId) { carts.findByIdAndUser(itemId, u).ifPresent(carts::delete); }

    @Transactional
    public void clear(User u) { carts.deleteAllByUser(u); }
}
