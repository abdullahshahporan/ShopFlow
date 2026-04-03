package com.shahporan.demo.strategy;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class PaymentResult {
    boolean success;
    String paymentStatus;
    PaymentMethod paymentMethod;
    BigDecimal amount;
    String message;

    public static PaymentResult success(PaymentMethod paymentMethod, BigDecimal amount, String paymentStatus, String message) {
        return PaymentResult.builder()
                .success(true)
                .paymentMethod(paymentMethod)
                .amount(amount)
                .paymentStatus(paymentStatus)
                .message(message)
                .build();
    }
}
