package com.shahporan.demo.repository;

import com.shahporan.demo.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerRepository extends JpaRepository<Seller, Long> {

    Optional<Seller> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByEnabled(Boolean enabled);

    List<Seller> findAllByOrderByCreatedAtDesc();
}
