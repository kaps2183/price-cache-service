package com.example.pricecache.service;

/**
 * Outcome of publishing a price to the cache.
 */
public enum PublishResult {
    /** The price was stored and distributed downstream. */
    ACCEPTED,
    /** A price with the same or a later timestamp is already cached; nothing changed. */
    IGNORED_OUT_OF_ORDER
}
