package com.srirammart.service;

import com.srirammart.model.OrderStatus;
import com.srirammart.model.Product;
import com.srirammart.model.Review;
import com.srirammart.model.User;
import com.srirammart.repo.OrderItemRepository;
import com.srirammart.repo.ProductRepository;
import com.srirammart.repo.ReviewRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {
    private final ReviewRepository reviews;
    private final ProductRepository products;
    private final OrderItemRepository orderItems;

    public ReviewService(ReviewRepository reviews, ProductRepository products, OrderItemRepository orderItems) {
        this.reviews = reviews;
        this.products = products;
        this.orderItems = orderItems;
    }

    public List<Review> latest(Product p, int n) { return reviews.findByProductOrderByCreatedAtDesc(p, PageRequest.of(0, n)); }
    public boolean hasReviewed(Product p, User u) { return reviews.existsByProductAndUser(p, u); }

    /** Share of 5..1 star reviews (index 0 = 5 stars) as whole percentages. */
    public int[] distribution(Product p) {
        List<Review> all = reviews.findByProduct(p);
        int[] pct = new int[5];
        if (all.isEmpty()) return pct;
        int[] n = new int[5];
        for (Review r : all) n[5 - Math.max(1, Math.min(5, r.getStars()))]++;
        for (int i = 0; i < 5; i++) pct[i] = Math.round(n[i] * 100f / all.size());
        return pct;
    }

    /** @return error message or null when saved */
    @Transactional
    public String add(User u, Long productId, int stars, String body) {
        Product p = products.findById(productId).orElse(null);
        if (p == null) return "Product not found.";
        if (stars < 1 || stars > 5) return "Choose a star rating from 1 to 5.";
        if (body == null || body.trim().length() < 3) return "Write a few words about the product.";
        if (body.length() > 1000) return "Review is too long (1000 characters max).";
        if (reviews.existsByProductAndUser(p, u)) return "You have already reviewed this product.";
        Review r = new Review();
        r.setProduct(p);
        r.setUser(u);
        r.setStars(stars);
        r.setBody(body.trim());
        r.setVerified(orderItems.countPurchases(u, p, OrderStatus.CANCELLED) > 0);
        reviews.save(r);
        int n = p.getRatingCount();
        p.setRating(Math.round(((p.getRating() * n + stars) / (n + 1)) * 10.0) / 10.0);
        p.setRatingCount(n + 1);
        products.save(p);
        return null;
    }
}
