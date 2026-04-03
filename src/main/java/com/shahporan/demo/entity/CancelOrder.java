package com.shahporan.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cancel_order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_order_id", nullable = false, unique = true)
    private Long originalOrderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Column(nullable = false)
    @Builder.Default
    private String status = "CANCELLED";

    @Column(length = 500)
    private String reason;

    @Column(nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "payment_method", nullable = false)
    @Builder.Default
    private String paymentMethod = "COD";

    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private String paymentStatus = "PENDING";

    @Column(name = "items_snapshot", columnDefinition = "TEXT")
    private String itemsSnapshot;

    @Column(name = "order_created_at")
    private LocalDateTime orderCreatedAt;

    @Column(name = "cancelled_at", updatable = false)
    private LocalDateTime cancelledAt;

    @PrePersist
    protected void onCreate() {
        this.cancelledAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "CANCELLED";
        }
        if (this.total == null) {
            this.total = BigDecimal.ZERO;
        }
        if (this.paymentMethod == null) {
            this.paymentMethod = "COD";
        }
        if (this.paymentStatus == null) {
            this.paymentStatus = "PENDING";
        }
    }
}
