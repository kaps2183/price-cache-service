package com.example.pricecache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Externalised cache settings ({@code price-cache.*}).
 *
 * @param retention        how long a price stays valid; defaults to 30 days
 * @param evictionInterval how often stale prices are purged; defaults to 1 hour
 */
@ConfigurationProperties(prefix = "price-cache")
public record PriceCacheProperties(Duration retention, Duration evictionInterval) {

    public static final Duration DEFAULT_RETENTION = Duration.ofDays(30);
    public static final Duration DEFAULT_EVICTION_INTERVAL = Duration.ofHours(1);

    public PriceCacheProperties {
        retention = positiveOrDefault(retention, DEFAULT_RETENTION, "retention");
        evictionInterval = positiveOrDefault(evictionInterval, DEFAULT_EVICTION_INTERVAL, "evictionInterval");
    }

    private static Duration positiveOrDefault(Duration value, Duration fallback, String name) {
        if (value == null) {
            return fallback;
        }
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("price-cache." + name + " must be positive but was " + value);
        }
        return value;
    }
}
