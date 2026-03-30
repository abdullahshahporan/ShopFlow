package com.shahporan.demo.repository;

import com.shahporan.demo.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

	@Query("select coalesce(sum(oi.qty), 0) from OrderItem oi where oi.product.seller.id = :sellerId")
	Long sumSoldUnitsBySellerId(@Param("sellerId") Long sellerId);

	@Query("select coalesce(sum(oi.unitPrice * oi.qty), 0) from OrderItem oi where oi.product.seller.id = :sellerId")
	BigDecimal sumRevenueBySellerId(@Param("sellerId") Long sellerId);

	@Query("""
		select oi
		from OrderItem oi
		join fetch oi.order o
		join fetch o.buyer b
		join oi.product p
		where p.seller.id = :sellerId
		order by o.createdAt desc
		""")
	List<OrderItem> findAllBySellerIdWithOrderAndBuyer(@Param("sellerId") Long sellerId);

	boolean existsByOrderIdAndProductSellerId(Long orderId, Long sellerId);
}
