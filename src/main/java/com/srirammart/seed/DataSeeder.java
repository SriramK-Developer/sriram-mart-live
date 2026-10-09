package com.srirammart.seed;

import com.srirammart.config.AppProperties;
import com.srirammart.model.*;
import com.srirammart.repo.*;
import com.srirammart.service.OrderService;
import com.srirammart.service.ProductService;
import com.srirammart.util.CsvParser;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * First-start data: categories and the admin account always; demo sellers, catalogue, customers, reviews,
 * coupons and order history only when app.seed-demo-data=true and the database has no sellers yet.
 */
@Component
public class DataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    public static final String SELLER_PASSWORD = "Seller@123";
    public static final String BUYER_PASSWORD = "Buyer@123";
    public static final String CUSTOMER_PASSWORD = "Demo@1234";

    private final AppProperties props;
    private final PasswordEncoder encoder;
    private final UserRepository users;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final ProductService productService;
    private final ReviewRepository reviews;
    private final OrderRepository orders;
    private final OrderService orderService;
    private final CartItemRepository carts;
    private final WishlistItemRepository wishlists;
    private final NotificationRepository notes;
    private final CouponRepository coupons;
    private final Random rnd = new Random(20260920L);

    public DataSeeder(AppProperties props, PasswordEncoder encoder, UserRepository users, CategoryRepository categories,
                      ProductRepository products, ProductService productService, ReviewRepository reviews, OrderRepository orders,
                      OrderService orderService, CartItemRepository carts, WishlistItemRepository wishlists,
                      NotificationRepository notes, CouponRepository coupons) {
        this.props = props;
        this.encoder = encoder;
        this.users = users;
        this.categories = categories;
        this.products = products;
        this.productService = productService;
        this.reviews = reviews;
        this.orders = orders;
        this.orderService = orderService;
        this.carts = carts;
        this.wishlists = wishlists;
        this.notes = notes;
        this.coupons = coupons;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureAdmin();
            if (categories.count() == 0) {
                loadCategories();
                log.info("Categories initialized ({} categories)", categories.count());
            }
            if (props.isSeedDemoData()) {
                seedDemo();
            }
        } catch (Exception e) {
            log.error("DataSeeder encountered an error, continuing app startup: {}", e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------------------------------ basics
    private void ensureAdmin() {
        if (users.countByRole(Role.ADMIN) > 0) return;
        User a = new User();
        a.setUsername(props.getAdmin().getUsername());
        a.setEmail(props.getAdmin().getEmail());
        a.setFullName("SriramMart Admin");
        a.setRole(Role.ADMIN);
        a.setPasswordHash(encoder.encode(props.getAdmin().getPassword()));
        users.save(a);
        log.info("Admin account '{}' created", a.getUsername());
    }

    private List<Map<String, String>> csv(String name) {
        try {
            ClassPathResource res = new ClassPathResource("data/" + name);
            if (res.exists()) {
                try (Reader r = new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8)) {
                    List<Map<String, String>> rows = CsvParser.readMaps(r);
                    if (!rows.isEmpty()) return rows;
                }
            }
        } catch (Exception e) {
            log.warn("Could not read classpath resource data/{}: {}", name, e.getMessage());
        }
        return java.util.Collections.emptyList();
    }

    private void loadCategories() {
        List<Map<String, String>> rows = csv("categories.csv");
        if (rows.isEmpty()) {
            rows = defaultCategories();
        }
        for (Map<String, String> r : rows) {
            String slug = r.get("slug");
            if (categories.findBySlug(slug).isEmpty()) {
                Category c = new Category();
                c.setSlug(slug);
                c.setName(r.get("name"));
                c.setIcon(r.get("icon"));
                c.setFacets(r.get("facets"));
                c.setPriceSteps(r.get("priceSteps"));
                c.setTagline(r.get("tagline"));
                c.setEyebrow(r.get("eyebrow"));
                c.setTitle(r.get("title"));
                c.setSubline(r.get("subline"));
                categories.save(c);
            }
        }
    }

    private List<Map<String, String>> defaultCategories() {
        List<Map<String, String>> list = new ArrayList<>();
        list.add(catMap("mobiles-tablets", "Mobiles & Tablets", "smartphone", "RAM,Storage,Brand", "5000,10000,20000,40000,80000", "Latest smartphones and tablets with unbeatable offers", "Mobiles & Tablets", "Smartphones & Tablets", "Discover top flagship and budget devices."));
        list.add(catMap("computers-accessories", "Computers & Accessories", "laptop", "RAM,Storage,Processor", "15000,30000,50000,80000,120000", "High-performance laptops desktops and peripherals", "Computers & Tech", "Laptops & Accessories", "Power your productivity and gaming."));
        list.add(catMap("fashion", "Fashion", "shirt", "Size,Color,Gender", "499,999,1999,3999,7999", "Trending apparel footwear and accessories", "Fashion & Apparel", "Men & Women Fashion", "Upgrade your style with top brands."));
        list.add(catMap("home-living", "Home & Living", "home", "Room,Material,Type", "499,1499,4999,14999,29999", "Modern appliances decor and kitchen essentials", "Home Essentials", "Home & Kitchen Living", "Make your living spaces cozy and stylish."));
        list.add(catMap("beauty-personal-care", "Beauty & Personal Care", "sparkles", "Skin Type,Form,Gender", "199,499,999,1999,4999", "Skincare cosmetics hair care and wellness", "Personal Care", "Beauty & Grooming", "Daily grooming and luxury skincare."));
        list.add(catMap("sports-fitness", "Sports & Fitness", "activity", "Activity,Type", "499,999,2499,4999,9999", "Gym equipment fitness gear and outdoor sports", "Fitness & Outdoor", "Sports & Fitness", "Gear up for your fitness goals."));
        list.add(catMap("books", "Books", "book", "Language,Binding,Genre", "199,399,799,1499,2999", "Bestsellers fiction non-fiction and academics", "Bookstore", "Bestsellers & Classics", "Expand your mind with great reads."));
        list.add(catMap("toys-games", "Toys & Games", "gift", "Age Group,Type", "299,699,1499,2999,5999", "Action figures board games puzzles and crafts", "Kids & Toys", "Toys & Family Games", "Endless fun and learning for kids."));
        list.add(catMap("groceries", "Groceries", "shopping-bag", "Type,Pack Size", "99,249,499,999,1999", "Daily essentials dairy snacks and pantry staples", "Daily Essentials", "Supermarket Groceries", "Fresh staples delivered to your door."));
        return list;
    }

    private Map<String, String> catMap(String slug, String name, String icon, String facets, String steps, String tag, String eye, String title, String sub) {
        Map<String, String> m = new HashMap<>();
        m.put("slug", slug); m.put("name", name); m.put("icon", icon); m.put("facets", facets);
        m.put("priceSteps", steps); m.put("tagline", tag); m.put("eyebrow", eye); m.put("title", title); m.put("subline", sub);
        return m;
    }

    // ------------------------------------------------------------------------------------------ demo data
    private void seedDemo() throws Exception {
        String sellerHash = encoder.encode(SELLER_PASSWORD);
        String buyerHash = encoder.encode(BUYER_PASSWORD);
        String customerHash = encoder.encode(CUSTOMER_PASSWORD);

        // sellers
        Map<String, String> sellerUsername = new HashMap<>();
        List<Map<String, String>> sellerRows = csv("sellers.csv");
        if (sellerRows.isEmpty()) {
            sellerRows = List.of(
                Map.of("username", "techhub", "key", "techhub", "store", "TechHub Electronics", "rating", "4.8", "ratings", "1420"),
                Map.of("username", "stylestudio", "key", "stylestudio", "store", "StyleStudio Fashion", "rating", "4.6", "ratings", "980"),
                Map.of("username", "homeplus", "key", "homeplus", "store", "HomePlus Living", "rating", "4.7", "ratings", "650"),
                Map.of("username", "bookworld", "key", "bookworld", "store", "BookWorld Official", "rating", "4.9", "ratings", "2100"),
                Map.of("username", "sportify", "key", "sportify", "store", "Sportify Pro Gear", "rating", "4.7", "ratings", "820"),
                Map.of("username", "freshkart", "key", "freshkart", "store", "FreshKart Groceries", "rating", "4.6", "ratings", "540")
            );
        }
        for (Map<String, String> r : sellerRows) {
            String uname = r.get("username");
            if (users.findByUsernameIgnoreCase(uname).isEmpty()) {
                User s = new User();
                s.setUsername(uname);
                s.setEmail(uname + "@srirammart.local");
                s.setFullName(r.get("store"));
                s.setStoreName(r.get("store"));
                s.setStoreRating(Double.parseDouble(r.get("rating")));
                s.setStoreRatings(Integer.parseInt(r.get("ratings")));
                s.setRole(Role.SELLER);
                s.setPasswordHash(sellerHash);
                s.setPhone("98" + (10000000 + rnd.nextInt(89999999)));
                users.save(s);
            }
            sellerUsername.put(r.get("key"), uname);
        }
        if (users.findByUsernameIgnoreCase("gadgetzone").isEmpty()) {
            User pending = new User();
            pending.setUsername("gadgetzone");
            pending.setEmail("gadgetzone@srirammart.local");
            pending.setFullName("GadgetZone Traders");
            pending.setStoreName("GadgetZone Traders");
            pending.setRole(Role.SELLER);
            pending.setApproved(false);
            pending.setStoreRating(0.0);
            pending.setStoreRatings(0);
            pending.setPasswordHash(sellerHash);
            users.save(pending);
        }

        // catalogue
        if (products.count() < 2500) {
            List<Map<String, String>> rows = csv("products.csv");
            if (!rows.isEmpty()) {
                for (Map<String, String> r : rows) r.put("seller", sellerUsername.getOrDefault(r.get("seller"), r.get("seller")));
                ProductService.ImportResult imp = productService.importRows(rows);
                log.info("Catalogue: {} products created, {} errors", imp.getCreated(), imp.getErrors().size());
                imp.getErrors().forEach(e -> log.warn("Import: {}", e));
            }
        }

        if (coupons.count() == 0) seedCoupons();
        List<User> customers = seedCustomers(buyerHash, customerHash);
        List<Product> all = products.findAll();
        if (!all.isEmpty() && !customers.isEmpty()) {
            if (reviews.count() < 10) seedReviews(all, customers);
            if (orders.count() < 10) seedOrders(all, customers);
            User sriramUser = users.findByUsernameIgnoreCase("sriram").orElse(null);
            if (sriramUser != null && orders.countByUser(sriramUser) < 3) {
                seedSriram(sriramUser, all);
            }
        }
    }

    private void seedCoupons() {
        coupon("SRI200", "Flat ₹200 off on orders above ₹1,999", 0, 200, 0, 1999);
        coupon("WELCOME10", "10% off (up to ₹500) on orders above ₹999", 10, 0, 500, 999);
        coupon("FESTIVE15", "15% off (up to ₹1,500) on orders above ₹4,999", 15, 0, 1500, 4999);
    }

    private void coupon(String code, String desc, int pct, int flat, int max, int min) {
        Coupon c = new Coupon();
        c.setCode(code);
        c.setDescription(desc);
        c.setDiscountPercent(pct);
        c.setFlatAmount(BigDecimal.valueOf(flat));
        c.setMaxDiscount(BigDecimal.valueOf(max));
        c.setMinOrder(BigDecimal.valueOf(min));
        c.setExpiresOn(LocalDate.now().plusYears(1));
        coupons.save(c);
    }

    private static final String[] FIRST = {"Aarav", "Vivaan", "Aditya", "Arjun", "Karthik", "Suresh", "Ramesh", "Vignesh", "Harish", "Manoj", "Naveen", "Prakash",
            "Dinesh", "Gokul", "Ashwin", "Bharath", "Saravanan", "Muthu", "Rahul", "Rohit", "Ananya", "Priya", "Divya", "Kavya", "Meena", "Lakshmi", "Deepa",
            "Nithya", "Swathi", "Pooja", "Sneha", "Anjali", "Keerthi", "Revathi", "Shalini", "Ishita", "Nisha", "Vikram", "Sanjay", "Tharun"};
    private static final String[] LAST = {"Kumar", "Raj", "Iyer", "Nair", "Reddy", "Pillai", "Menon", "Sharma", "Gupta", "Patel", "Singh", "Krishnan",
            "Subramanian", "Murugan", "Selvam", "Rao", "Das", "Joshi", "Chandran", "Natarajan", "Balaji", "Ganesh"};
    private static final String[][] PLACES = {
            {"Chennai", "Tamil Nadu", "600001"}, {"Coimbatore", "Tamil Nadu", "641001"}, {"Madurai", "Tamil Nadu", "625001"},
            {"Thoothukudi", "Tamil Nadu", "628001"}, {"Tiruchirappalli", "Tamil Nadu", "620001"}, {"Tirunelveli", "Tamil Nadu", "627001"},
            {"Bengaluru", "Karnataka", "560001"}, {"Kochi", "Kerala", "682001"}, {"Hyderabad", "Telangana", "500001"},
            {"Mumbai", "Maharashtra", "400001"}, {"Pune", "Maharashtra", "411001"}, {"New Delhi", "Delhi", "110001"}, {"Kolkata", "West Bengal", "700001"}};

    private List<User> seedCustomers(String buyerHash, String customerHash) {
        List<User> out = new ArrayList<>();
        User s = users.findByUsernameIgnoreCase("sriram").orElse(null);
        if (s == null) {
            s = new User();
            s.setUsername("sriram");
            s.setEmail("sriram@srirammart.local");
            s.setFullName("Sriram K");
            s.setPhone("9876543210");
            s.setPasswordHash(buyerHash);
            s.setAddressLine("12, Gandhi Street, Anna Nagar");
            s.setCity("Chennai");
            s.setStateName("Tamil Nadu");
            s.setPincode("600001");
            s = users.save(s);
        }
        out.add(s);
        if (users.countByRole(Role.BUYER) <= 1) {
            java.util.Set<String> used = new java.util.HashSet<>(List.of("sriram"));
            for (int i = 0; i < 60; i++) {
                String f = FIRST[rnd.nextInt(FIRST.length)], l = LAST[rnd.nextInt(LAST.length)];
                String uname = (f + "." + l).toLowerCase();
                int n = 1;
                String candidate = uname;
                while (!used.add(candidate)) candidate = uname + (++n);
                String[] place = PLACES[rnd.nextInt(PLACES.length)];
                User u = new User();
                u.setUsername(candidate);
                u.setEmail(candidate + "@example.com");
                u.setFullName(f + " " + l);
                u.setPhone("9" + (100000000 + rnd.nextInt(899999999)));
                u.setPasswordHash(customerHash);
                u.setAddressLine((1 + rnd.nextInt(180)) + ", " + LAST[rnd.nextInt(LAST.length)] + " Nagar, Main Road");
                u.setCity(place[0]);
                u.setStateName(place[1]);
                u.setPincode(place[2]);
                u.setCreatedAt(LocalDateTime.now().minusDays(20 + rnd.nextInt(300)));
                out.add(users.save(u));
            }
        } else {
            out.addAll(users.findByRoleOrderByCreatedAtDesc(Role.BUYER));
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------ reviews
    private static final String[] GOOD = {"Great value for money. Highly recommended!", "Excellent quality and it arrived earlier than expected.",
            "Exactly as described. Very happy with this purchase.", "Works perfectly, no complaints so far.", "Worth every rupee. Packaging was neat too.",
            "Good build quality and the seller was quick to dispatch."};
    private static final String[] OKAY = {"Decent product for the price, packaging could be better.", "Good overall but delivery took a day longer than expected.",
            "Does the job. A few small things could be improved."};
    private static final String[] POOR = {"Average quality, expected a bit more for the price.", "It is okay, but not as good as I hoped."};
    private static final Map<String, String[]> BY_CATEGORY = Map.of(
            "mobiles-tablets", new String[]{"Battery easily lasts a full day and the display is bright.", "Smooth performance for daily use and the camera is good in daylight."},
            "computers-accessories", new String[]{"Fast and smooth for multitasking. The build feels solid.", "Comfortable to use for long hours, setup was easy."},
            "fashion", new String[]{"Fabric is soft and the fit is true to size.", "Looks even better in person, got many compliments."},
            "home-living", new String[]{"Sturdy build and it looks great in the room.", "Easy to set up and the finish is very good."},
            "beauty-personal-care", new String[]{"Gentle on skin and the fragrance is light.", "Visible results after two weeks of regular use."},
            "sports-fitness", new String[]{"Comfortable and well built, great for daily workouts.", "Solid quality, I have been using it every day."},
            "toys-games", new String[]{"My kids love it, kept them busy for hours.", "Good quality pieces, sturdy and safe."},
            "books", new String[]{"Easy to read and full of practical ideas.", "Could not put it down. Well worth reading."},
            "groceries", new String[]{"Fresh and well packed.", "Good quality and it reached on time."});

    private void seedReviews(List<Product> all, List<User> customers) {
        for (Product p : all) {
            int n = 6 + rnd.nextInt(5);
            java.util.Set<Integer> picked = new java.util.HashSet<>();
            for (int i = 0; i < n; i++) {
                int idx = 1 + rnd.nextInt(customers.size() - 1);
                if (!picked.add(idx)) continue;
                int stars = stars(p.getRating());
                String[] pool = stars >= 4 ? GOOD : stars == 3 ? OKAY : POOR;
                String text = rnd.nextInt(3) == 0 ? BY_CATEGORY.get(p.getCategory().getSlug())[rnd.nextInt(2)] : pool[rnd.nextInt(pool.length)];
                Review r = new Review();
                r.setProduct(p);
                r.setUser(customers.get(idx));
                r.setStars(stars);
                r.setBody(text);
                r.setVerified(rnd.nextInt(10) < 7);
                r.setHelpful(rnd.nextInt(140));
                r.setCreatedAt(LocalDateTime.now().minusDays(1 + rnd.nextInt(150)).minusHours(rnd.nextInt(24)));
                reviews.save(r);
            }
        }
    }

    private int stars(double rating) {
        int[] w = rating >= 4.5 ? new int[]{60, 25, 10, 3, 2} : rating >= 4.2 ? new int[]{45, 30, 15, 6, 4} : new int[]{35, 30, 20, 9, 6};
        int x = rnd.nextInt(100), acc = 0;
        for (int i = 0; i < 5; i++) { acc += w[i]; if (x < acc) return 5 - i; }
        return 5;
    }

    // ------------------------------------------------------------------------------------------ orders
    private void seedOrders(List<Product> all, List<User> customers) {
        List<Product> sellable = new ArrayList<>();
        for (Product p : all) if (p.getStock() > 20) sellable.add(p);
        for (int i = 0; i < 150; i++) {
            User buyer = customers.get(1 + rnd.nextInt(customers.size() - 1));
            int lines = 1 + rnd.nextInt(3);
            List<Object[]> picks = new ArrayList<>();
            java.util.Set<Long> ids = new java.util.HashSet<>();
            for (int k = 0; k < lines; k++) {
                Product p = sellable.get(rnd.nextInt(sellable.size()));
                if (ids.add(p.getId())) picks.add(new Object[]{p, 1 + (rnd.nextInt(4) == 0 ? 1 : 0)});
            }
            int ageHours = 3 + rnd.nextInt(45 * 24);
            LocalDateTime when = LocalDateTime.now().minusHours(ageHours);
            OrderStatus st;
            int roll = rnd.nextInt(100);
            if (ageHours > 8 * 24) st = roll < 90 ? OrderStatus.DELIVERED : OrderStatus.CANCELLED;
            else if (ageHours > 3 * 24) st = roll < 55 ? OrderStatus.DELIVERED : roll < 80 ? OrderStatus.OUT_FOR_DELIVERY : roll < 92 ? OrderStatus.SHIPPED : OrderStatus.CANCELLED;
            else if (ageHours > 24) st = roll < 40 ? OrderStatus.SHIPPED : roll < 75 ? OrderStatus.CONFIRMED : roll < 95 ? OrderStatus.PLACED : OrderStatus.CANCELLED;
            else st = roll < 55 ? OrderStatus.PLACED : OrderStatus.CONFIRMED;
            PaymentMethod pm = PaymentMethod.values()[rnd.nextInt(PaymentMethod.values().length)];
            makeOrder(buyer, picks, when, st, pm, null);
        }
    }

    private Order makeOrder(User buyer, List<Object[]> lines, LocalDateTime when, OrderStatus status, PaymentMethod pm, String fixedNo) {
        Order o = new Order();
        o.setOrderNo("TMP" + System.nanoTime());
        o.setUser(buyer);
        o.setPaymentMethod(pm);
        o.setCreatedAt(when);
        o.setUpdatedAt(when.plusHours(2));
        BigDecimal mrp = BigDecimal.ZERO, price = BigDecimal.ZERO;
        for (Object[] l : lines) {
            Product p = (Product) l[0];
            int q = (Integer) l[1];
            try { products.decrementStock(p.getId(), q); } catch (Exception ignored) {}
            OrderItem oi = new OrderItem();
            oi.setProduct(p);
            oi.setSeller(p.getSeller());
            oi.setProductName(p.getName());
            oi.setSubtitle(p.getSubtitle());
            oi.setImagePath(p.getImageMain());
            oi.setUnitPrice(p.getPrice());
            oi.setMrp(p.getMrp());
            oi.setQuantity(q);
            oi.setLineTotal(p.getPrice().multiply(BigDecimal.valueOf(q)));
            oi.setStatus(status);
            o.addItem(oi);
            mrp = mrp.add(p.getMrp().multiply(BigDecimal.valueOf(q)));
            price = price.add(oi.getLineTotal());
        }
        if (o.getItems().isEmpty()) return null;
        BigDecimal delivery = price.compareTo(BigDecimal.valueOf(499)) >= 0 ? BigDecimal.ZERO : BigDecimal.valueOf(40);
        o.setSubtotal(mrp);
        o.setDiscount(mrp.subtract(price));
        o.setDeliveryCharge(delivery);
        o.setTotal(price.add(delivery));
        o.setStatus(status);
        boolean paid = pm != PaymentMethod.COD || status == OrderStatus.DELIVERED;
        o.setPaymentStatus(status == OrderStatus.CANCELLED ? (pm == PaymentMethod.COD ? PaymentStatus.PENDING : PaymentStatus.REFUNDED)
                : paid ? PaymentStatus.PAID : PaymentStatus.PENDING);
        o.setShipName(buyer.getFullName() != null ? buyer.getFullName() : "Sriram K");
        o.setShipPhone(buyer.getPhone() != null ? buyer.getPhone() : "9876543210");
        o.setShipAddress(buyer.getAddressLine() != null ? buyer.getAddressLine() : "12, Anna Nagar");
        o.setShipCity(buyer.getCity() != null ? buyer.getCity() : "Chennai");
        o.setShipState(buyer.getStateName() != null ? buyer.getStateName() : "Tamil Nadu");
        o.setShipPincode(buyer.getPincode() != null ? buyer.getPincode() : "600001");
        o.setExpectedDelivery(when.toLocalDate().plusDays(OrderService.etaDays(o.getShipPincode())));
        o = orders.save(o);
        o.setOrderNo(fixedNo != null && !orders.existsByOrderNo(fixedNo) ? fixedNo : orderService.nextOrderNo(o.getId()));
        return orders.save(o);
    }

    // ------------------------------------------------------------------------------------------ demo shopper
    private static List<Object[]> lines(Object[]... ls) { return new ArrayList<>(java.util.Arrays.asList(ls)); }

    private Product byName(List<Product> all, String sku) {
        if (all == null || all.isEmpty()) return null;
        for (Product p : all) if (p.getSku().equalsIgnoreCase(sku)) return p;
        int idx = Math.abs(sku.hashCode()) % all.size();
        return all.get(idx);
    }

    private void seedSriram(User u, List<Product> all) {
        if (all == null || all.isEmpty()) return;
        Product p1 = all.get(0);
        Product p2 = all.size() > 1 ? all.get(1) : p1;
        Product p3 = all.size() > 2 ? all.get(2) : p1;
        Product p4 = all.size() > 3 ? all.get(3) : p1;
        Product p5 = all.size() > 4 ? all.get(4) : p1;

        LocalDateTime now = LocalDateTime.now();
        Order shipped = makeOrder(u, lines(new Object[]{p1, 1}), now.minusHours(40), OrderStatus.SHIPPED, PaymentMethod.UPI, "SM123456");
        Order confirmed = makeOrder(u, lines(new Object[]{p2, 1}), now.minusHours(26), OrderStatus.CONFIRMED, PaymentMethod.COD, "SM123455");
        Order delivered = makeOrder(u, lines(new Object[]{p3, 1}, new Object[]{p4, 1}), now.minusDays(6), OrderStatus.DELIVERED, PaymentMethod.CARD, "SM123442");

        List<Product> cartProducts = List.of(p1, p2, p3, p4);
        for (Product p : cartProducts) {
            CartItem c = new CartItem();
            c.setUser(u);
            c.setProduct(p);
            c.setQuantity(1);
            carts.save(c);
        }

        List<Product> wishProducts = List.of(p1, p2, p3, p4, p5);
        for (Product p : wishProducts) {
            WishlistItem w = new WishlistItem();
            w.setUser(u);
            w.setProduct(p);
            wishlists.save(w);
        }

        if (shipped != null) note(u, NotificationType.ORDER, "Order update", "Your order #" + shipped.getOrderNo() + " has been shipped. It will be delivered by " + shipped.getExpectedDelivery() + ".", "/orders/" + shipped.getOrderNo(), "View Order", now.minusHours(2));
        note(u, NotificationType.OFFER, "Offer alert", "Get up to 40% OFF on Computers & Accessories! Limited time offer – don't miss out!", "/category/computers-accessories", "Shop Now", now.minusHours(5));
        note(u, NotificationType.OFFER, "Wishlist price drop", p1.getName() + " is now available at ₹" + p1.getPrice() + ".", "/product/" + p1.getId(), "View Product", now.minusHours(7));
        if (confirmed != null) note(u, NotificationType.ORDER, "Order confirmed", "Your order #" + confirmed.getOrderNo() + " has been confirmed. Estimated delivery: " + confirmed.getExpectedDelivery() + ".", "/orders/" + confirmed.getOrderNo(), "View Order", now.minusHours(25));
        note(u, NotificationType.ACCOUNT, "Account security", "Your password was successfully changed.", null, null, now.minusHours(28));
        note(u, NotificationType.OFFER, "Promotion", "Flat ₹200 OFF on orders above ₹1,999. Use code: SRI200", "/deals", "Shop Now", now.minusDays(2));
        if (delivered != null) note(u, NotificationType.ORDER, "Order delivered", "Your order #" + delivered.getOrderNo() + " has been delivered. We hope you loved your purchase!", "/orders/" + delivered.getOrderNo(), "View Order", now.minusDays(3));
        note(u, NotificationType.SYSTEM, "New feature", "Try our AI Assistant! Get instant answers to your shopping queries.", "/", "Chat Now", now.minusDays(4));
    }

    private void note(User u, NotificationType type, String title, String msg, String link, String label, LocalDateTime at) {
        Notification n = new Notification();
        n.setUser(u);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(msg);
        n.setLink(link);
        n.setLinkLabel(label);
        n.setCreatedAt(at);
        notes.save(n);
    }
}
