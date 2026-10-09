package com.srirammart.repo;

import com.srirammart.model.Category;
import com.srirammart.model.Product;
import com.srirammart.model.User;
import java.util.List;
import org.springframework.data.domain.Page;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySku(String sku);

    /** Atomic stock decrement. Returns 0 when there is not enough stock, so overselling is impossible. */
    @Transactional
    @Modifying
    @Query("update Product p set p.stock = p.stock - :q, p.soldCount = p.soldCount + :q where p.id = :id and p.stock >= :q")
    int decrementStock(@Param("id") Long id, @Param("q") int q);

    @Transactional
    @Modifying
    @Query("update Product p set p.stock = p.stock + :q, p.soldCount = p.soldCount - :q where p.id = :id")
    int restoreStock(@Param("id") Long id, @Param("q") int q);

    List<Product> findByFeaturedTrueAndActiveTrueOrderByIdAsc();
    List<Product> findByActiveTrueAndCategoryOrderByIdAsc(Category category);
    List<Product> findBySellerOrderByIdDesc(User seller);
    List<Product> findByStockLessThanEqualAndActiveTrueOrderByStockAsc(int stock, Pageable pageable);
    List<Product> findByStockLessThanEqualAndSellerAndActiveTrueOrderByStockAsc(int stock, User seller, Pageable pageable);
    List<Product> findAllByOrderByIdDesc(Pageable pageable);
    List<Product> findTop6ByActiveTrueOrderBySoldCountDesc();
    List<Product> findTop5BySellerOrderBySoldCountDesc(User seller);
    Page<Product> findByNameContainingIgnoreCaseOrSkuContainingIgnoreCaseOrderByIdDesc(String name, String sku, Pageable pageable);
    long countByActiveTrue();
    long countBySeller(User seller);

    @Query("select p from Product p where p.active = true and p.mrp > p.price order by (p.mrp - p.price) / p.mrp desc, p.ratingCount desc")
    List<Product> findTopDeals(Pageable pageable);

    @Query("select p from Product p where p.active = true and p.category = :c and p.id <> :id order by p.ratingCount desc")
    List<Product> findRelated(@Param("c") Category c, @Param("id") Long id, Pageable pageable);

    @Query("select p.subCategory, count(p) from Product p where p.category = :c and p.active = true group by p.subCategory order by count(p) desc")
    List<Object[]> countBySubCategory(@Param("c") Category c);

    @Query("select p.category.id, p.mrp, p.price from Product p where p.active = true and p.mrp > p.price")
    List<Object[]> discountedPrices();
}
