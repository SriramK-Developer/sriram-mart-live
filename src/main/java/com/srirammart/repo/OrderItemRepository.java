package com.srirammart.repo;

import com.srirammart.model.OrderItem;
import com.srirammart.model.OrderStatus;
import com.srirammart.model.Product;
import com.srirammart.model.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findBySellerOrderByIdDesc(User seller);

    @Query("select count(i) from OrderItem i where i.order.user = :u and i.product = :p and i.status <> :cancelled")
    long countPurchases(@Param("u") User u, @Param("p") Product p, @Param("cancelled") OrderStatus cancelled);

    @Query("select i from OrderItem i where i.order.createdAt >= :after")
    List<OrderItem> findSince(@Param("after") LocalDateTime after);

    @Query("select i from OrderItem i where i.seller = :s and i.order.createdAt >= :after")
    List<OrderItem> findSinceForSeller(@Param("s") User s, @Param("after") LocalDateTime after);

    @Query("select sum(i.lineTotal) from OrderItem i where i.seller = :s and i.status <> :cancelled")
    BigDecimal sellerRevenue(@Param("s") User s, @Param("cancelled") OrderStatus cancelled);

    @Query("select sum(i.quantity) from OrderItem i where i.seller = :s and i.status <> :cancelled")
    Long sellerUnits(@Param("s") User s, @Param("cancelled") OrderStatus cancelled);
}
