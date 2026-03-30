package com.shahporan.demo.strategy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class NagadPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentMethod method() {
        return PaymentMethod.NAGAD;
    }

    @Override
    public PaymentResult process(BigDecimal amount, Long buyerId) {
        return PaymentResult.success(method(), amount, "PAID", "Nagad payment successful");
    }
}
