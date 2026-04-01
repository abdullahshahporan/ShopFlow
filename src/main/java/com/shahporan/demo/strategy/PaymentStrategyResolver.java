package com.shahporan.demo.strategy;

import com.shahporan.demo.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentStrategyResolver {

    private final Map<PaymentMethod, PaymentStrategy> strategyMap = new EnumMap<>(PaymentMethod.class);

    public PaymentStrategyResolver(List<PaymentStrategy> strategies) {
        for (PaymentStrategy strategy : strategies) {
            strategyMap.put(strategy.method(), strategy);
        }
    }

    public PaymentStrategy resolve(String paymentMethod) {
        PaymentMethod method;
        try {
            method = PaymentMethod.fromValue(paymentMethod);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }

        PaymentStrategy strategy = strategyMap.get(method);
        if (strategy == null) {
            throw new BadRequestException("No payment strategy found for method: " + paymentMethod);
        }
        return strategy;
    }
}
