package com.shahporan.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

//import com.shahporan.demo.entity.Seller;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String sku;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(length = 500)
    private String imageUrl;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "product")
    private Stock stock;

    public void setStock(Stock stock) {
        this.stock = stock;
        if (stock != null && stock.getProduct() != this) {
            stock.setProduct(this);
        }
    }

    /**
     * Returns the current stock quantity tracked by the Stock table.
     * Product catalog info (name, price, SKU) lives here; quantity lives in Stock.
     */
    public Integer getQuantity() {
        Stock currentStock = getStock();
        Integer quantity = currentStock != null ? currentStock.getQuantity() : null;
        return quantity != null ? quantity : 0;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.active == null) {
            this.active = true;
        }
    }
}
