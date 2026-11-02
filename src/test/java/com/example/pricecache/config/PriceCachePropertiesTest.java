package com.example.pricecache.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@DisplayName("PriceCacheProperties")
class PriceCachePropertiesTest {

    @Nested
    @DisplayName("Given no configuration")
    class GivenNoConfiguration {

        @Test
        @DisplayName("when bound, then retention defaults to 30 days and eviction to hourly")
        void defaults() {
            PriceCacheProperties properties = new PriceCacheProperties(null, null);

            assertThat(properties.retention()).isEqualTo(Duration.ofDays(30));
            assertThat(properties.evictionInterval()).isEqualTo(Duration.ofHours(1));
        }
    }

    @Nested
    @DisplayName("Given explicit configuration")
    class GivenExplicitConfiguration {

        @Test
        @DisplayName("when bound, then the configured values are kept")
        void keepsValues() {
            PriceCacheProperties properties = new PriceCacheProperties(Duration.ofDays(7), Duration.ofMinutes(5));

            assertThat(properties.retention()).isEqualTo(Duration.ofDays(7));
            assertThat(properties.evictionInterval()).isEqualTo(Duration.ofMinutes(5));
        }

        @Test
        @DisplayName("when the retention is not positive, then binding fails")
        void rejectsNonPositiveRetention() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new PriceCacheProperties(Duration.ZERO, null))
                    .withMessageContaining("retention");
        }

        @Test
        @DisplayName("when the eviction interval is not positive, then binding fails")
        void rejectsNonPositiveInterval() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new PriceCacheProperties(null, Duration.ofSeconds(-1)))
                    .withMessageContaining("evictionInterval");
        }
    }
}
