package com.example.pricecache.service;

import com.example.pricecache.config.PriceCacheProperties;
import com.example.pricecache.domain.Price;
import com.example.pricecache.repository.PriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static com.example.pricecache.support.PriceFixtures.NOW;
import static com.example.pricecache.support.PriceFixtures.price;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("PriceCacheService")
class PriceCacheServiceTest {

    private static final Duration RETENTION = Duration.ofDays(30);
    private static final Instant CUTOFF = NOW.minus(RETENTION);

    @Mock
    private PriceRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PriceCacheService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        PriceCacheProperties properties = new PriceCacheProperties(RETENTION, Duration.ofHours(1));
        service = new PriceCacheService(repository, eventPublisher, clock, properties);
    }

    @Nested
    @DisplayName("Given a vendor publishes a fresh price")
    class GivenFreshPrice {

        private final Price price = price("BLOOMBERG", "AAPL", NOW.minusSeconds(5));

        @Test
        @DisplayName("when it is newer than the cached one, then it is stored and distributed downstream")
        void acceptedAndDistributed() {
            given(repository.saveIfNewer(price)).willReturn(true);

            PublishResult result = service.publish(price);

            assertThat(result).isEqualTo(PublishResult.ACCEPTED);
            then(repository).should().saveIfNewer(price);
            then(eventPublisher).should().publishEvent(new PricePublishedEvent(price));
        }

        @Test
        @DisplayName("when the cache already holds a newer price, then it is ignored and not distributed")
        void ignoredOutOfOrder() {
            given(repository.saveIfNewer(price)).willReturn(false);

            PublishResult result = service.publish(price);

            assertThat(result).isEqualTo(PublishResult.IGNORED_OUT_OF_ORDER);
            then(eventPublisher).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("when it is exactly at the retention boundary, then it is still accepted")
        void boundaryAccepted() {
            Price boundary = price("BLOOMBERG", "AAPL", CUTOFF);
            given(repository.saveIfNewer(boundary)).willReturn(true);

            assertThat(service.publish(boundary)).isEqualTo(PublishResult.ACCEPTED);
        }
    }

    @Nested
    @DisplayName("Given a vendor publishes a price older than the retention period")
    class GivenStalePrice {

        @Test
        @DisplayName("when published, then it is rejected without touching the cache")
        void rejected() {
            Price stale = price("BLOOMBERG", "AAPL", CUTOFF.minusMillis(1));

            assertThatThrownBy(() -> service.publish(stale))
                    .isInstanceOf(StalePriceException.class)
                    .hasMessageContaining("BLOOMBERG")
                    .hasMessageContaining("AAPL");
            then(repository).shouldHaveNoInteractions();
            then(eventPublisher).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("Given a client retrieves prices")
    class GivenClientRetrieves {

        private final Price fresh = price("BLOOMBERG", "AAPL", NOW);
        private final Price notYetEvicted = price("BLOOMBERG", "MSFT", CUTOFF.minusSeconds(1));

        @Test
        @DisplayName("when asking for a vendor, then its prices are returned without stale ones awaiting eviction")
        void byVendor() {
            given(repository.findByVendor("BLOOMBERG")).willReturn(List.of(fresh, notYetEvicted));

            assertThat(service.getPricesByVendor("BLOOMBERG")).containsExactly(fresh);
        }

        @Test
        @DisplayName("when asking for an instrument, then every vendor's fresh price is returned")
        void byInstrument() {
            Price reuters = price("REUTERS", "AAPL", NOW);
            Price staleIce = price("ICE", "AAPL", CUTOFF.minusSeconds(1));
            given(repository.findByInstrument("AAPL")).willReturn(List.of(fresh, staleIce, reuters));

            assertThat(service.getPricesByInstrument("AAPL")).containsExactly(fresh, reuters);
        }

        @Test
        @DisplayName("when nothing is cached, then an empty list is returned")
        void empty() {
            given(repository.findByVendor(any())).willReturn(List.of());

            assertThat(service.getPricesByVendor("UNKNOWN")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Given the eviction is triggered")
    class GivenEviction {

        @Test
        @DisplayName("when it runs, then prices older than the retention cutoff are deleted")
        void evicts() {
            given(repository.deleteOlderThan(CUTOFF)).willReturn(3);

            int removed = service.evictStalePrices();

            assertThat(removed).isEqualTo(3);
            then(repository).should().deleteOlderThan(CUTOFF);
        }
    }
}
