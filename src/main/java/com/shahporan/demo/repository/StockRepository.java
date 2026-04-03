package com.shahporan.demo.repository;

import com.shahporan.demo.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByProductId(Long productId);

    List<Stock> findByProductIdIn(Collection<Long> productIds);

    List<Stock> findBySellerId(Long sellerId);
}
