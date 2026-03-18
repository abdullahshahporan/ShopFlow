package com.shahporan.demo.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class OrderItemResponseDto {
    Long productId;
    String productName;
    Integer qty;
    BigDecimal unitPrice;
    BigDecimal subtotal;
}
