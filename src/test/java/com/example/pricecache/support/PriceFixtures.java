package com.example.pricecache.support;

import com.example.pricecache.domain.Price;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Test data builders shared across the test suite.
 */
public final class PriceFixtures {

    public static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

    private PriceFixtures() {
    }

    public static Price price(String vendorId, String instrumentId, Instant timestamp) {
        return price(vendorId, instrumentId, "100.00", "100.10", timestamp);
    }

    public static Price price(String vendorId, String instrumentId, String bid, String ask, Instant timestamp) {
        return new Price(vendorId, instrumentId, new BigDecimal(bid), new BigDecimal(ask), "USD", timestamp);
    }
}
