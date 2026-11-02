package com.example.pricecache.web;

import com.example.pricecache.domain.Price;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Inbound payload for publishing a vendor price.
 */
public record PublishPriceRequest(
        @NotBlank String vendorId,
        @NotBlank String instrumentId,
        @NotNull @Positive BigDecimal bid,
        @NotNull @Positive BigDecimal ask,
        @NotNull @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter ISO currency code") String currency,
        @NotNull Instant timestamp) {

    public Price toDomain() {
        return new Price(vendorId, instrumentId, bid, ask, currency, timestamp);
    }
}
