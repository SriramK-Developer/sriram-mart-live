package com.srirammart.service;

import com.srirammart.model.Product;
import com.srirammart.model.User;
import com.srirammart.model.WishlistItem;
import com.srirammart.repo.ProductRepository;
import com.srirammart.repo.WishlistItemRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishlistService {
    private final WishlistItemRepository repo;
    private final ProductRepository products;

    public WishlistService(WishlistItemRepository repo, ProductRepository products) {
        this.repo = repo;
        this.products = products;
    }

    public List<WishlistItem> items(User u) { return repo.findByUserOrderByCreatedAtDesc(u); }
    public long count(User u) { return repo.countByUser(u); }
    public Set<Long> ids(User u) { return new HashSet<>(repo.findProductIdsByUser(u)); }

    /** @return true when the product is now in the wishlist */
    @Transactional
    public boolean toggle(User u, Long productId) {
        Product p = products.findById(productId).orElseThrow();
        var existing = repo.findByUserAndProduct(u, p);
        if (existing.isPresent()) { repo.delete(existing.get()); return false; }
        WishlistItem w = new WishlistItem();
        w.setUser(u);
        w.setProduct(p);
        repo.save(w);
        return true;
    }

    @Transactional
    public void clear(User u) { repo.deleteAllByUser(u); }
}
