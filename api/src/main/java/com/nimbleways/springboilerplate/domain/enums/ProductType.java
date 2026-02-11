package com.nimbleways.springboilerplate.domain.enums;

public enum ProductType {
    NORMAL,
    SEASONAL,
    EXPIRABLE;

    public static ProductType fromString(String type) {
        try {
            return ProductType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown product type: " + type);
        }
    }
}
