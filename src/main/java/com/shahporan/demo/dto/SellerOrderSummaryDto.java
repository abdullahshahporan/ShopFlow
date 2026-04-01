package com.shahporan.demo.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class SellerOrderSummaryDto {
    Long orderId;
    String buyerName;
    String paymentMethod;
    String paymentStatus;
    String status;
    Integer totalUnits;
    BigDecimal totalAmount;
    LocalDateTime createdAt;
}
