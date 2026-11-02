package com.example.pricecache.distribution;

import com.example.pricecache.domain.Price;
import com.example.pricecache.service.PricePublishedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.example.pricecache.support.PriceFixtures.NOW;
import static com.example.pricecache.support.PriceFixtures.price;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("DownstreamPriceDistributor")
class DownstreamPriceDistributorTest {

    private final Price price = price("BLOOMBERG", "AAPL", NOW);

    @Mock
    private PriceSubscriber riskSystem;

    @Mock
    private PriceSubscriber tradingSystem;

    @Nested
    @DisplayName("Given several downstream subscribers")
    class GivenSubscribers {

        @Test
        @DisplayName("when a price is published, then every subscriber receives it")
        void fanOut() {
            DownstreamPriceDistributor distributor = new DownstreamPriceDistributor(List.of(riskSystem, tradingSystem));

            distributor.onPricePublished(new PricePublishedEvent(price));

            then(riskSystem).should().onPrice(price);
            then(tradingSystem).should().onPrice(price);
        }

        @Test
        @DisplayName("when one subscriber fails, then the others still receive the price")
        void isolatesFailures() {
            willThrow(new IllegalStateException("downstream offline")).given(riskSystem).onPrice(price);
            DownstreamPriceDistributor distributor = new DownstreamPriceDistributor(List.of(riskSystem, tradingSystem));

            assertThatCode(() -> distributor.onPricePublished(new PricePublishedEvent(price)))
                    .doesNotThrowAnyException();
            then(tradingSystem).should().onPrice(price);
        }
    }

    @Nested
    @DisplayName("Given no downstream subscribers")
    class GivenNoSubscribers {

        @Test
        @DisplayName("when a price is published, then nothing happens")
        void noop() {
            DownstreamPriceDistributor distributor = new DownstreamPriceDistributor(List.of());

            assertThatCode(() -> distributor.onPricePublished(new PricePublishedEvent(price)))
                    .doesNotThrowAnyException();
        }
    }
}
