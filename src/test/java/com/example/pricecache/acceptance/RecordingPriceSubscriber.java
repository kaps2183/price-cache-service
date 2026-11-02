package com.example.pricecache.acceptance;

import com.example.pricecache.distribution.PriceSubscriber;
import com.example.pricecache.domain.Price;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test double standing in for a downstream system.
 */
public class RecordingPriceSubscriber implements PriceSubscriber {

    private final List<Price> received = new CopyOnWriteArrayList<>();

    @Override
    public String name() {
        return "recording";
    }

    @Override
    public void onPrice(Price price) {
        received.add(price);
    }

    public List<Price> received() {
        return List.copyOf(received);
    }

    public void clear() {
        received.clear();
    }
}
