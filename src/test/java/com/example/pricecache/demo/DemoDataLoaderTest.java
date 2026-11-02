package com.example.pricecache.demo;

import com.example.pricecache.domain.Price;
import com.example.pricecache.service.PriceCacheService;
import com.example.pricecache.service.PublishResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;

import static com.example.pricecache.support.PriceFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("DemoDataLoader")
class DemoDataLoaderTest {

    @Mock
    private PriceCacheService service;

    @Test
    @DisplayName("Given the demo profile, when the application starts, then sample prices from 3 vendors for 3 instruments are published")
    void seeds() {
        given(service.publish(any(Price.class))).willReturn(PublishResult.ACCEPTED);
        DemoDataLoader loader = new DemoDataLoader(service, Clock.fixed(NOW, ZoneOffset.UTC));

        loader.run();

        ArgumentCaptor<Price> captor = ArgumentCaptor.forClass(Price.class);
        then(service).should(times(9)).publish(captor.capture());
        assertThat(captor.getAllValues()).extracting(Price::vendorId).containsOnly("BLOOMBERG", "REUTERS", "ICE");
        assertThat(captor.getAllValues()).extracting(Price::instrumentId).containsOnly("AAPL", "MSFT", "VOD.L");
        assertThat(captor.getAllValues()).allSatisfy(p -> assertThat(p.timestamp()).isBeforeOrEqualTo(NOW));
    }
}
