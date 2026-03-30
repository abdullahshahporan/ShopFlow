package com.shahporan.demo.strategy;

public enum PaymentMethod {
    COD,
    BKASH,
    NAGAD,
    CARD;

    public static PaymentMethod fromValue(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Payment method is required");
        }
        String normalized = raw.trim().toUpperCase();
        try {
            return PaymentMethod.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported payment method: " + raw);
        }
    }
}
