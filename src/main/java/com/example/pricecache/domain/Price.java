package com.example.pricecache.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable quote for an instrument as reported by a single vendor at a point in time.
 *
 * @param vendorId     the price vendor (e.g. BLOOMBERG)
 * @param instrumentId the traded instrument (e.g. AAPL)
 * @param bid          best bid, must be positive and not exceed {@code ask}
 * @param ask          best ask, must be positive
 * @param currency     ISO-4217 currency code
 * @param timestamp    when the vendor produced the price; drives ordering and retention
 */
public record Price(String vendorId,
                    String instrumentId,
                    BigDecimal bid,
                    BigDecimal ask,
                    String currency,
                    Instant timestamp) {

    public Price {
        new PriceKey(vendorId, instrumentId);
        requirePositive(bid, "bid");
        requirePositive(ask, "ask");
        PriceKey.requireText(currency, "currency");
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp must not be null");
        }
        if (bid.compareTo(ask) > 0) {
            throw new IllegalArgumentException("bid must not exceed ask");
        }
    }

    public PriceKey key() {
        return new PriceKey(vendorId, instrumentId);
    }

    public boolean isOlderThan(Instant cutoff) {
        return timestamp.isBefore(cutoff);
    }

    public boolean isNewerThan(Price other) {
        return timestamp.isAfter(other.timestamp);
    }

    private static void requirePositive(BigDecimal value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
