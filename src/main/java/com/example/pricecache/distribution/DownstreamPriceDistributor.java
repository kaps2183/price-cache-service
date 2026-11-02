package com.example.pricecache.distribution;

import com.example.pricecache.domain.Price;
import com.example.pricecache.service.PricePublishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Fans every cached price out to all registered {@link PriceSubscriber}s. A failing subscriber is
 * logged and skipped so it cannot block the others or the publishing vendor.
 */
@Component
public class DownstreamPriceDistributor {

    private static final Logger log = LoggerFactory.getLogger(DownstreamPriceDistributor.class);

    private final List<PriceSubscriber> subscribers;

    public DownstreamPriceDistributor(List<PriceSubscriber> subscribers) {
        this.subscribers = List.copyOf(subscribers);
        log.info("Distributing prices to {} subscriber(s)", this.subscribers.size());
    }

    @EventListener
    public void onPricePublished(PricePublishedEvent event) {
        Price price = event.price();
        for (PriceSubscriber subscriber : subscribers) {
            try {
                subscriber.onPrice(price);
                log.debug("Delivered {} to {}", price.key(), subscriber.name());
            } catch (RuntimeException e) {
                log.error("Subscriber {} failed to receive {}", subscriber.name(), price.key(), e);
            }
        }
    }
}
