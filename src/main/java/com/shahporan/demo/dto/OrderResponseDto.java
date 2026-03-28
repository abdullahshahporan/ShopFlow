package com.shahporan.demo.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class OrderResponseDto {
    Long id;
    Long buyerId;
    String buyerName;
    String status;
    BigDecimal total;
    List<OrderItemResponseDto> items;
    LocalDateTime createdAt;
}
