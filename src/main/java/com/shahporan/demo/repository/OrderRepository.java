package com.shahporan.demo.repository;

import com.shahporan.demo.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByBuyerId(Long buyerId);

    List<Order> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    List<Order> findByProductSellerIdOrderByCreatedAtDesc(Long sellerId);

    Optional<Order> findByIdAndBuyerId(Long id, Long buyerId);

    boolean existsByIdAndProductSellerId(Long id, Long sellerId);

    long countByBuyerId(Long buyerId);

    @Query("select coalesce(sum(o.total), 0) from Order o where o.buyer.id = :buyerId")
    BigDecimal sumTotalByBuyerId(@Param("buyerId") Long buyerId);

    @Query("select coalesce(sum(o.total), 0) from Order o")
    BigDecimal sumTotalAllOrders();

    @Query("select coalesce(sum(o.qty), 0) from Order o where o.product.seller.id = :sellerId")
    Long sumSoldUnitsBySellerId(@Param("sellerId") Long sellerId);

    @Query("select coalesce(sum(o.total), 0) from Order o where o.product.seller.id = :sellerId")
    BigDecimal sumRevenueBySellerId(@Param("sellerId") Long sellerId);
}
