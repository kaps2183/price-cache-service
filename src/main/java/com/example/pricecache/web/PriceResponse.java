package com.example.pricecache.web;

import com.example.pricecache.domain.Price;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Outbound representation of a cached price, decoupling the API contract from the domain model.
 */
public record PriceResponse(String vendorId,
                            String instrumentId,
                            BigDecimal bid,
                            BigDecimal ask,
                            String currency,
                            Instant timestamp) {

    public static PriceResponse from(Price price) {
        return new PriceResponse(price.vendorId(), price.instrumentId(), price.bid(), price.ask(),
                price.currency(), price.timestamp());
    }
}
