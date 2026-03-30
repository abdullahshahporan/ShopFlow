package com.shahporan.demo.repository;

import com.shahporan.demo.entity.CancelOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CancelOrderRepository extends JpaRepository<CancelOrder, Long> {

    List<CancelOrder> findByBuyerIdOrderByCancelledAtDesc(Long buyerId);

    boolean existsByOriginalOrderId(Long originalOrderId);
}
