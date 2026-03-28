package com.shahporan.demo.repository;

import com.shahporan.demo.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findBySellerId(Long sellerId);

    List<Product> findBySellerIdAndActiveTrue(Long sellerId);

    List<Product> findByActiveTrue();

    boolean existsBySku(String sku);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    Optional<Product> findByIdAndActiveTrue(Long id);
}
