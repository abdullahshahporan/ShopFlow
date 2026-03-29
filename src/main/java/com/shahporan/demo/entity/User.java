package com.shahporan.demo.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_role_enabled", columnList = "role_int, enabled"),
    @Index(name = "idx_users_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    /**
     * 0 = BUYER, 1 = SELLER, 2 = ADMIN
     */
    @Column(name = "role_int", nullable = false)
    private Integer roleInt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /* ── Relationships ── */
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Product> products = new ArrayList<>();

    @OneToMany(mappedBy = "buyer", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.enabled == null) {
            this.enabled = true;
        }
        if (this.roleInt == null) {
            this.roleInt = 0;
        }
    }
}
