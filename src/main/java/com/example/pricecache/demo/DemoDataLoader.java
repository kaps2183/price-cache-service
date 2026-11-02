package com.example.pricecache.demo;

import com.example.pricecache.domain.Price;
import com.example.pricecache.service.PriceCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Seeds the cache with sample prices when started with {@code --spring.profiles.active=demo}.
 */
@Component
@Profile("demo")
public class DemoDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private static final List<String> VENDORS = List.of("BLOOMBERG", "REUTERS", "ICE");
    private static final List<Instrument> INSTRUMENTS = List.of(
            new Instrument("AAPL", "189.10", "USD"),
            new Instrument("MSFT", "410.00", "USD"),
            new Instrument("VOD.L", "72.10", "GBP"));

    private final PriceCacheService service;
    private final Clock clock;

    public DemoDataLoader(PriceCacheService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        Instant now = clock.instant();
        for (int v = 0; v < VENDORS.size(); v++) {
            // Each vendor quotes slightly differently and at a slightly different time.
            BigDecimal skew = new BigDecimal("0.05").multiply(BigDecimal.valueOf(v));
            Instant timestamp = now.minus(Duration.ofSeconds(v * 30L));
            for (Instrument instrument : INSTRUMENTS) {
                BigDecimal bid = new BigDecimal(instrument.bid()).add(skew);
                BigDecimal ask = bid.add(new BigDecimal("0.10"));
                service.publish(new Price(VENDORS.get(v), instrument.id(), bid, ask, instrument.currency(), timestamp));
            }
        }
        log.info("Demo data loaded: {} vendors x {} instruments", VENDORS.size(), INSTRUMENTS.size());
    }

    private record Instrument(String id, String bid, String currency) {
    }
}
