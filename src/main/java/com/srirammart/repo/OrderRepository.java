package com.srirammart.repo;

import com.srirammart.model.Order;
import com.srirammart.model.OrderStatus;
import com.srirammart.model.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserOrderByCreatedAtDesc(User user);
    Optional<Order> findByOrderNo(String orderNo);
    Optional<Order> findByOrderNoAndUser(String orderNo, User user);
    boolean existsByOrderNo(String orderNo);
    long countByUser(User user);
    long countByStatus(OrderStatus status);
    List<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<Order> findByCreatedAtAfter(LocalDateTime after);

    @Query("select distinct o from Order o join o.items i where i.seller = :s order by o.createdAt desc")
    List<Order> findBySeller(@Param("s") User seller);

    @Query("select sum(o.total) from Order o where o.status <> :cancelled")
    BigDecimal revenue(@Param("cancelled") OrderStatus cancelled);

    @Query("select count(distinct o.id) from Order o join o.items i where i.seller = :s")
    long countForSeller(@Param("s") User seller);
}
