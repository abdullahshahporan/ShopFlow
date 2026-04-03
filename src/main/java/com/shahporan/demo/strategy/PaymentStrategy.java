package com.shahporan.demo.strategy;

import java.math.BigDecimal;

public interface PaymentStrategy {

    PaymentMethod method();

    PaymentResult process(BigDecimal amount, Long buyerId);
}
