package com.srirammart.repo;

import com.srirammart.model.Product;
import com.srirammart.model.Review;
import com.srirammart.model.User;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProductOrderByCreatedAtDesc(Product product, Pageable pageable);
    List<Review> findByProduct(Product product);
    boolean existsByProductAndUser(Product product, User user);
    long countByProduct(Product product);
}
