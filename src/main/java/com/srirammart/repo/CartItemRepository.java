package com.srirammart.repo;

import com.srirammart.model.CartItem;
import com.srirammart.model.Product;
import com.srirammart.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserOrderByIdAsc(User user);
    Optional<CartItem> findByUserAndProduct(User user, Product product);
    Optional<CartItem> findByIdAndUser(Long id, User user);
    long countByUser(User user);

    @Transactional
    @Modifying
    @Query("delete from CartItem c where c.user = :u")
    void deleteAllByUser(@Param("u") User u);
}
