package com.example.pricecache.service;

import com.example.pricecache.domain.Price;

import java.time.Instant;

/**
 * Thrown when a vendor publishes a price that is already outside the retention window.
 */
public class StalePriceException extends RuntimeException {

    public StalePriceException(Price price, Instant cutoff) {
        super("Price %s/%s at %s is older than the retention cutoff %s"
                .formatted(price.vendorId(), price.instrumentId(), price.timestamp(), cutoff));
    }
}
