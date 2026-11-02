package com.example.pricecache.distribution;

import com.example.pricecache.domain.Price;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default downstream stand-in that logs each update; replace or add real transports alongside it.
 */
@Component
public class LoggingPriceSubscriber implements PriceSubscriber {

    private static final Logger log = LoggerFactory.getLogger(LoggingPriceSubscriber.class);

    @Override
    public String name() {
        return "logging";
    }

    @Override
    public void onPrice(Price price) {
        log.info("[downstream] {} {} bid={} ask={} {} at {}", price.vendorId(), price.instrumentId(),
                price.bid().toPlainString(), price.ask().toPlainString(), price.currency(), price.timestamp());
    }
}
