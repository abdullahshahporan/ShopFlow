package com.shahporan.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    /**
     * PENDING, CONFIRMED, CANCELLED
     */
    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "payment_method", nullable = false)
    @Builder.Default
    private String paymentMethod = "COD";

    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private String paymentStatus = "PENDING";

    @Column(nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    /* ── Relationships ── */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "PENDING";
        }
        if (this.paymentMethod == null) {
            this.paymentMethod = "COD";
        }
        if (this.paymentStatus == null) {
            this.paymentStatus = "PENDING";
        }
        if (this.total == null) {
            this.total = BigDecimal.ZERO;
        }
    }
}
