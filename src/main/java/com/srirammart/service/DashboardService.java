package com.srirammart.service;

import com.srirammart.model.Category;
import com.srirammart.model.OrderItem;
import com.srirammart.model.OrderStatus;
import com.srirammart.model.Role;
import com.srirammart.model.User;
import com.srirammart.repo.OrderItemRepository;
import com.srirammart.repo.OrderRepository;
import com.srirammart.repo.ProductRepository;
import com.srirammart.repo.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private final UserRepository users;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final OrderItemRepository items;

    public DashboardService(UserRepository users, ProductRepository products, OrderRepository orders, OrderItemRepository items) {
        this.users = users;
        this.products = products;
        this.orders = orders;
        this.items = items;
    }

    public static class Bar {
        private final String label;
        private final BigDecimal value;
        private final int pct;
        Bar(String label, BigDecimal value, int pct) { this.label = label; this.value = value; this.pct = pct; }
        public String getLabel() { return label; }
        public BigDecimal getValue() { return value; }
        public int getPct() { return pct; }
    }

    public Map<String, Object> admin() {
        Map<String, Object> m = new LinkedHashMap<>();
        LocalDateTime since = LocalDate.now().minusDays(29).atStartOfDay();
        List<OrderItem> recent = items.findSince(since);
        BigDecimal rev = orders.revenue(OrderStatus.CANCELLED);
        m.put("revenue", rev == null ? BigDecimal.ZERO : rev);
        m.put("orderCount", orders.count());
        m.put("buyerCount", users.countByRole(Role.BUYER));
        m.put("sellerCount", users.countByRole(Role.SELLER));
        m.put("productCount", products.countByActiveTrue());
        List<User> pending = new ArrayList<>();
        for (User s : users.findByRoleOrderByCreatedAtDesc(Role.SELLER)) if (!s.isApproved()) pending.add(s);
        m.put("pendingSellers", pending);
        m.put("lowStock", products.findByStockLessThanEqualAndActiveTrueOrderByStockAsc(10, PageRequest.of(0, 8)));
        m.put("recentOrders", orders.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 8)));
        m.put("revenueBars", series(recent, 14));
        m.put("categoryBars", categoryBars(recent));
        m.put("topProducts", products.findTop6ByActiveTrueOrderBySoldCountDesc());
        Map<String, Long> status = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) status.put(s.getLabel(), orders.countByStatus(s));
        m.put("statusCounts", status);
        return m;
    }

    public Map<String, Object> seller(User s) {
        Map<String, Object> m = new LinkedHashMap<>();
        LocalDateTime since = LocalDate.now().minusDays(29).atStartOfDay();
        List<OrderItem> recent = items.findSinceForSeller(s, since);
        BigDecimal rev = items.sellerRevenue(s, OrderStatus.CANCELLED);
        m.put("revenue", rev == null ? BigDecimal.ZERO : rev);
        m.put("orderCount", orders.countForSeller(s));
        Long units = items.sellerUnits(s, OrderStatus.CANCELLED);
        m.put("units", units == null ? 0L : units);
        m.put("productCount", products.countBySeller(s));
        m.put("lowStock", products.findByStockLessThanEqualAndSellerAndActiveTrueOrderByStockAsc(10, s, PageRequest.of(0, 8)));
        m.put("revenueBars", series(recent, 14));
        m.put("topProducts", products.findTop5BySellerOrderBySoldCountDesc(s));
        List<OrderItem> all = items.findBySellerOrderByIdDesc(s);
        m.put("recentItems", all.size() > 8 ? all.subList(0, 8) : all);
        long open = 0;
        for (OrderItem i : all) if (!i.getStatus().isFinal()) open++;
        m.put("openItems", open);
        return m;
    }

    private List<Bar> series(List<OrderItem> list, int days) {
        LocalDate today = LocalDate.now();
        Map<LocalDate, BigDecimal> map = new LinkedHashMap<>();
        for (int i = days - 1; i >= 0; i--) map.put(today.minusDays(i), BigDecimal.ZERO);
        for (OrderItem it : list) {
            if (it.getStatus() == OrderStatus.CANCELLED) continue;
            LocalDate d = it.getOrder().getCreatedAt().toLocalDate();
            if (map.containsKey(d)) map.merge(d, it.getLineTotal(), BigDecimal::add);
        }
        return bars(map, d -> d.format(DAY));
    }

    private List<Bar> categoryBars(List<OrderItem> list) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (OrderItem it : list) {
            if (it.getStatus() == OrderStatus.CANCELLED) continue;
            Category c = it.getProduct().getCategory();
            map.merge(c.getName(), it.getLineTotal(), BigDecimal::add);
        }
        Map<String, BigDecimal> sorted = new LinkedHashMap<>();
        map.entrySet().stream().sorted((a, b) -> b.getValue().compareTo(a.getValue())).forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return bars(sorted, k -> k);
    }

    private <K> List<Bar> bars(Map<K, BigDecimal> map, java.util.function.Function<K, String> label) {
        BigDecimal max = map.values().stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ONE);
        if (max.signum() == 0) max = BigDecimal.ONE;
        List<Bar> out = new ArrayList<>();
        for (Map.Entry<K, BigDecimal> e : map.entrySet()) {
            int pct = e.getValue().multiply(BigDecimal.valueOf(100)).divide(max, 0, RoundingMode.HALF_UP).intValue();
            out.add(new Bar(label.apply(e.getKey()), e.getValue(), e.getValue().signum() > 0 ? Math.max(pct, 3) : 0));
        }
        return out;
    }
}
