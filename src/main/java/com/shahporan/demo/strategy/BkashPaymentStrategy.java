package com.shahporan.demo.strategy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BkashPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentMethod method() {
        return PaymentMethod.BKASH;
    }

    @Override
    public PaymentResult process(BigDecimal amount, Long buyerId) {
        return PaymentResult.success(method(), amount, "PAID", "Bkash payment successful");
    }
}
