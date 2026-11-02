package com.example.pricecache.domain;

/**
 * Identity of a cached price: one price per instrument per vendor.
 */
public record PriceKey(String vendorId, String instrumentId) {

    public PriceKey {
        requireText(vendorId, "vendorId");
        requireText(instrumentId, "instrumentId");
    }

    static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
