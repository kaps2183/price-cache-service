package com.example.pricecache.distribution;

import com.example.pricecache.domain.Price;

/**
 * A downstream system interested in price updates. Register an implementation as a Spring bean
 * (e.g. one that forwards to Kafka, JMS or a WebSocket) and it will receive every cached price.
 */
public interface PriceSubscriber {

    String name();

    void onPrice(Price price);
}
