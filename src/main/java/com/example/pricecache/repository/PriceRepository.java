package com.example.pricecache.repository;

import com.example.pricecache.domain.Price;
import com.example.pricecache.domain.PriceKey;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistence port for cached prices. The service depends only on this interface, so the
 * in-memory implementation can be swapped for a database-backed one without touching callers.
 */
public interface PriceRepository {

    /**
     * Atomically stores {@code price} if no price is cached for its key, or if it is newer
     * than the cached one.
     *
     * @return {@code true} if the price was stored, {@code false} if it was older or a duplicate
     */
    boolean saveIfNewer(Price price);

    Optional<Price> findByKey(PriceKey key);

    /** All prices from one vendor, ordered by instrument id. */
    List<Price> findByVendor(String vendorId);

    /** One price per vendor for the instrument, ordered by vendor id. */
    List<Price> findByInstrument(String instrumentId);

    /**
     * Removes every price whose timestamp is strictly before {@code cutoff}.
     *
     * @return the number of prices removed
     */
    int deleteOlderThan(Instant cutoff);

    long count();
}
