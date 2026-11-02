package com.example.pricecache.service;

import com.example.pricecache.config.PriceCacheProperties;
import com.example.pricecache.domain.Price;
import com.example.pricecache.repository.PriceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Entry point for publishing and retrieving cached prices. Enforces the retention rule and
 * announces every stored price so it can be distributed to downstream systems.
 */
@Service
public class PriceCacheService {

    private static final Logger log = LoggerFactory.getLogger(PriceCacheService.class);

    private final PriceRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final Duration retention;

    public PriceCacheService(PriceRepository repository,
                             ApplicationEventPublisher eventPublisher,
                             Clock clock,
                             PriceCacheProperties properties) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.retention = properties.retention();
    }

    public PublishResult publish(Price price) {
        Instant cutoff = retentionCutoff();
        if (price.isOlderThan(cutoff)) {
            log.warn("Rejected stale price {} at {} (cutoff {})", price.key(), price.timestamp(), cutoff);
            throw new StalePriceException(price, cutoff);
        }
        if (!repository.saveIfNewer(price)) {
            log.info("Ignored out-of-order price {} at {}", price.key(), price.timestamp());
            return PublishResult.IGNORED_OUT_OF_ORDER;
        }
        log.info("Cached price {} bid={} ask={} at {}", price.key(), price.bid(), price.ask(), price.timestamp());
        eventPublisher.publishEvent(new PricePublishedEvent(price));
        return PublishResult.ACCEPTED;
    }

    public List<Price> getPricesByVendor(String vendorId) {
        List<Price> prices = withinRetention(repository.findByVendor(vendorId));
        log.debug("Returning {} prices for vendor {}", prices.size(), vendorId);
        return prices;
    }

    public List<Price> getPricesByInstrument(String instrumentId) {
        List<Price> prices = withinRetention(repository.findByInstrument(instrumentId));
        log.debug("Returning {} prices for instrument {}", prices.size(), instrumentId);
        return prices;
    }

    public int evictStalePrices() {
        Instant cutoff = retentionCutoff();
        int removed = repository.deleteOlderThan(cutoff);
        log.info("Evicted {} prices older than {}", removed, cutoff);
        return removed;
    }

    /**
     * Eviction runs periodically, so a price may age past the cutoff between runs; filtering on
     * read guarantees clients never see a price older than the retention period.
     */
    private List<Price> withinRetention(List<Price> prices) {
        Instant cutoff = retentionCutoff();
        return prices.stream().filter(price -> !price.isOlderThan(cutoff)).toList();
    }

    private Instant retentionCutoff() {
        return clock.instant().minus(retention);
    }
}
