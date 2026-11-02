package com.example.pricecache.scheduling;

import com.example.pricecache.service.PriceCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically removes prices older than the retention period from the cache.
 */
@Component
public class StalePriceEvictionJob {

    private static final Logger log = LoggerFactory.getLogger(StalePriceEvictionJob.class);

    private final PriceCacheService service;

    public StalePriceEvictionJob(PriceCacheService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${price-cache.eviction-interval:PT1H}",
               initialDelayString = "${price-cache.eviction-interval:PT1H}")
    public void evictStalePrices() {
        log.debug("Stale price eviction started");
        try {
            int removed = service.evictStalePrices();
            log.debug("Stale price eviction finished, {} removed", removed);
        } catch (RuntimeException e) {
            // Keep the schedule alive; the next run will retry.
            log.error("Stale price eviction failed", e);
        }
    }
}
