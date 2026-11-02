package com.example.pricecache.repository;

import com.example.pricecache.domain.Price;
import com.example.pricecache.domain.PriceKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Lock-free in-memory store keeping two indexes over the same prices:
 * vendor -> instrument -> price and instrument -> vendor -> price, so both client queries
 * are a single map lookup.
 * <p>
 * Every update is an atomic newer-wins merge on each index, so the two indexes converge on
 * the newest price whatever order concurrent updates arrive in. Removal is conditional on the
 * exact stale price, so a newer price arriving during eviction is never deleted.
 */
@Repository
public class InMemoryPriceRepository implements PriceRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryPriceRepository.class);

    private final ConcurrentMap<String, ConcurrentMap<String, Price>> byVendor = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ConcurrentMap<String, Price>> byInstrument = new ConcurrentHashMap<>();

    @Override
    public boolean saveIfNewer(Price price) {
        if (!putIfNewer(byVendor, price.vendorId(), price.instrumentId(), price)) {
            log.debug("Not storing {} at {}: cached price is the same age or newer", price.key(), price.timestamp());
            return false;
        }
        putIfNewer(byInstrument, price.instrumentId(), price.vendorId(), price);
        log.debug("Stored {} at {}", price.key(), price.timestamp());
        return true;
    }

    @Override
    public Optional<Price> findByKey(PriceKey key) {
        Map<String, Price> instruments = byVendor.get(key.vendorId());
        return Optional.ofNullable(instruments == null ? null : instruments.get(key.instrumentId()));
    }

    @Override
    public List<Price> findByVendor(String vendorId) {
        return snapshot(byVendor, vendorId, Price::instrumentId);
    }

    @Override
    public List<Price> findByInstrument(String instrumentId) {
        return snapshot(byInstrument, instrumentId, Price::vendorId);
    }

    @Override
    public int deleteOlderThan(Instant cutoff) {
        int removed = 0;
        for (Map<String, Price> instruments : byVendor.values()) {
            for (Price price : instruments.values()) {
                if (price.isOlderThan(cutoff) && removeIfSame(byVendor, price.vendorId(), price.instrumentId(), price)) {
                    removeIfSame(byInstrument, price.instrumentId(), price.vendorId(), price);
                    removed++;
                }
            }
        }
        log.debug("Deleted {} prices older than {}", removed, cutoff);
        return removed;
    }

    @Override
    public long count() {
        return byVendor.values().stream().mapToLong(Map::size).sum();
    }

    /**
     * Stores {@code price} under outer/inner unless the cached price is the same age or newer.
     * The inner update runs inside the outer {@code compute}, so it cannot race with
     * {@link #removeIfSame} dropping an empty inner map.
     */
    private static boolean putIfNewer(ConcurrentMap<String, ConcurrentMap<String, Price>> index,
                                      String outerKey, String innerKey, Price price) {
        AtomicBoolean stored = new AtomicBoolean();
        index.compute(outerKey, (k, prices) -> {
            ConcurrentMap<String, Price> target = prices != null ? prices : new ConcurrentHashMap<>();
            target.compute(innerKey, (ik, existing) -> {
                if (existing == null || price.isNewerThan(existing)) {
                    stored.set(true);
                    return price;
                }
                return existing;
            });
            return target;
        });
        return stored.get();
    }

    /**
     * Removes {@code price} only if it is still the cached value, and drops the inner map once
     * it is empty so unused vendors and instruments do not accumulate.
     */
    private static boolean removeIfSame(ConcurrentMap<String, ConcurrentMap<String, Price>> index,
                                        String outerKey, String innerKey, Price price) {
        AtomicBoolean removed = new AtomicBoolean();
        index.computeIfPresent(outerKey, (k, prices) -> {
            removed.set(prices.remove(innerKey, price));
            return prices.isEmpty() ? null : prices;
        });
        return removed.get();
    }

    private static List<Price> snapshot(ConcurrentMap<String, ConcurrentMap<String, Price>> index,
                                        String id, Function<Price, String> sortKey) {
        Map<String, Price> prices = index.get(id);
        if (prices == null) {
            return List.of();
        }
        return prices.values().stream().sorted(Comparator.comparing(sortKey)).toList();
    }
}
