package com.srirammart.repo;

import com.srirammart.model.Product;
import com.srirammart.model.User;
import com.srirammart.model.WishlistItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findByUserOrderByCreatedAtDesc(User user);
    Optional<WishlistItem> findByUserAndProduct(User user, Product product);
    long countByUser(User user);

    @Query("select w.product.id from WishlistItem w where w.user = :u")
    List<Long> findProductIdsByUser(@Param("u") User u);

    @Transactional
    @Modifying
    @Query("delete from WishlistItem w where w.user = :u")
    void deleteAllByUser(@Param("u") User u);
}
