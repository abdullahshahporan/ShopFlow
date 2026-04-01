package com.shahporan.demo.strategy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CodPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentMethod method() {
        return PaymentMethod.COD;
    }

    @Override
    public PaymentResult process(BigDecimal amount, Long buyerId) {
        return PaymentResult.success(method(), amount, "PENDING", "Cash on Delivery selected");
    }
}
