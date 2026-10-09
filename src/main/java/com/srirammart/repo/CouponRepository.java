package com.srirammart.repo;

import com.srirammart.model.Coupon;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCodeIgnoreCase(String code);
    List<Coupon> findAllByOrderByIdDesc();
    List<Coupon> findByActiveTrueOrderByIdAsc();
}
