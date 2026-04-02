package com.shahporan.demo.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class ProductResponseDto {
    Long id;
    Long sellerId;
    String sellerName;
    String name;
    String sku;
    BigDecimal price;
    Integer quantity;
    boolean active;
    String imageUrl;
    LocalDateTime createdAt;
}
