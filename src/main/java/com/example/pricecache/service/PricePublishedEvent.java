package com.example.pricecache.service;

import com.example.pricecache.domain.Price;

/**
 * Raised after a price has been stored in the cache.
 */
public record PricePublishedEvent(Price price) {
}
