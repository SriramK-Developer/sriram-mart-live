package com.srirammart.service;

import com.srirammart.model.Coupon;
import com.srirammart.repo.CouponRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponService {
    private final CouponRepository repo;

    public CouponService(CouponRepository repo) { this.repo = repo; }

    public static class Result {
        private final Coupon coupon;
        private final BigDecimal discount;
        private final String error;
        Result(Coupon c, BigDecimal d, String e) { this.coupon = c; this.discount = d; this.error = e; }
        public Coupon getCoupon() { return coupon; }
        public BigDecimal getDiscount() { return discount; }
        public String getError() { return error; }
        public boolean isValid() { return error == null && coupon != null; }
    }

    public Result evaluate(String code, BigDecimal orderValue) {
        if (code == null || code.isBlank()) return new Result(null, BigDecimal.ZERO, "Enter a coupon code.");
        Coupon c = repo.findByCodeIgnoreCase(code.trim()).orElse(null);
        if (c == null || !c.isActive()) return new Result(null, BigDecimal.ZERO, "That coupon code is not valid.");
        if (c.isExpired()) return new Result(null, BigDecimal.ZERO, "That coupon has expired.");
        if (c.getMinOrder() != null && orderValue.compareTo(c.getMinOrder()) < 0)
            return new Result(null, BigDecimal.ZERO, "Add items worth ₹" + c.getMinOrder().setScale(0, RoundingMode.HALF_UP) + " or more to use " + c.getCode() + ".");
        BigDecimal d;
        if (c.getFlatAmount() != null && c.getFlatAmount().signum() > 0) d = c.getFlatAmount();
        else d = orderValue.multiply(BigDecimal.valueOf(c.getDiscountPercent())).divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
        if (c.getMaxDiscount() != null && c.getMaxDiscount().signum() > 0 && d.compareTo(c.getMaxDiscount()) > 0) d = c.getMaxDiscount();
        if (d.compareTo(orderValue) > 0) d = orderValue;
        return new Result(c, d, null);
    }

    public List<Coupon> all() { return repo.findAllByOrderByIdDesc(); }
    public List<Coupon> active() {
        return repo.findByActiveTrueOrderByIdAsc().stream().filter(c -> !c.isExpired()).toList();
    }

    @Transactional
    public void markUsed(String code) {
        repo.findByCodeIgnoreCase(code).ifPresent(c -> { c.setUsedCount(c.getUsedCount() + 1); repo.save(c); });
    }

    @Transactional
    public String create(String code, String description, int percent, BigDecimal flat, BigDecimal max, BigDecimal min, LocalDate expires) {
        if (code == null || !code.trim().matches("[A-Za-z0-9]{3,20}")) return "Coupon code must be 3-20 letters or numbers.";
        if (repo.findByCodeIgnoreCase(code.trim()).isPresent()) return "A coupon with this code already exists.";
        if (percent <= 0 && (flat == null || flat.signum() <= 0)) return "Set either a percentage or a flat amount.";
        if (percent > 90) return "Percentage cannot be more than 90.";
        Coupon c = new Coupon();
        c.setCode(code.trim().toUpperCase());
        c.setDescription(description);
        c.setDiscountPercent(Math.max(0, percent));
        c.setFlatAmount(flat == null ? BigDecimal.ZERO : flat);
        c.setMaxDiscount(max == null ? BigDecimal.ZERO : max);
        c.setMinOrder(min == null ? BigDecimal.ZERO : min);
        c.setExpiresOn(expires);
        repo.save(c);
        return null;
    }

    @Transactional
    public void toggle(Long id) { repo.findById(id).ifPresent(c -> { c.setActive(!c.isActive()); repo.save(c); }); }
}
