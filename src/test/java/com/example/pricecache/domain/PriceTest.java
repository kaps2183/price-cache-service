package com.example.pricecache.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static com.example.pricecache.support.PriceFixtures.NOW;
import static com.example.pricecache.support.PriceFixtures.price;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@DisplayName("Price")
class PriceTest {

    @Nested
    @DisplayName("Given valid attributes")
    class GivenValidAttributes {

        @Test
        @DisplayName("when created, then it exposes its vendor/instrument key")
        void exposesKey() {
            Price price = price("BLOOMBERG", "AAPL", NOW);

            assertThat(price.key()).isEqualTo(new PriceKey("BLOOMBERG", "AAPL"));
        }

        @Test
        @DisplayName("when bid equals ask, then it is accepted (locked market)")
        void bidEqualsAsk() {
            Price price = price("BLOOMBERG", "AAPL", "100.00", "100.00", NOW);

            assertThat(price.bid()).isEqualByComparingTo(price.ask());
        }
    }

    @Nested
    @DisplayName("Given invalid attributes")
    class GivenInvalidAttributes {

        @Test
        @DisplayName("when the vendor id is blank, then construction is rejected")
        void blankVendor() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> price(" ", "AAPL", NOW))
                    .withMessageContaining("vendorId");
        }

        @Test
        @DisplayName("when the bid is missing, then construction is rejected")
        void missingBid() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new Price("BLOOMBERG", "AAPL", null, BigDecimal.ONE, "USD", NOW))
                    .withMessageContaining("bid");
        }

        @Test
        @DisplayName("when the bid is not positive, then construction is rejected")
        void nonPositiveBid() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> price("BLOOMBERG", "AAPL", "0", "1.00", NOW))
                    .withMessageContaining("positive");
        }

        @Test
        @DisplayName("when the bid exceeds the ask, then construction is rejected")
        void crossedMarket() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> price("BLOOMBERG", "AAPL", "101.00", "100.00", NOW))
                    .withMessageContaining("bid must not exceed ask");
        }

        @Test
        @DisplayName("when the currency is blank, then construction is rejected")
        void blankCurrency() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new Price("BLOOMBERG", "AAPL", BigDecimal.ONE, BigDecimal.TEN, "", NOW))
                    .withMessageContaining("currency");
        }

        @Test
        @DisplayName("when the timestamp is missing, then construction is rejected")
        void missingTimestamp() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> price("BLOOMBERG", "AAPL", null))
                    .withMessageContaining("timestamp");
        }
    }

    @Nested
    @DisplayName("Given prices at different times")
    class GivenPricesAtDifferentTimes {

        private final Price older = price("BLOOMBERG", "AAPL", NOW.minus(Duration.ofMinutes(5)));
        private final Price newer = price("BLOOMBERG", "AAPL", NOW);

        @Test
        @DisplayName("when compared, then the later one is newer and not vice versa")
        void newerThan() {
            assertThat(newer.isNewerThan(older)).isTrue();
            assertThat(older.isNewerThan(newer)).isFalse();
            assertThat(newer.isNewerThan(newer)).isFalse();
        }

        @Test
        @DisplayName("when checked against a cutoff, then only prices strictly before it are older")
        void olderThanCutoff() {
            Instant cutoff = NOW.minus(Duration.ofMinutes(1));

            assertThat(older.isOlderThan(cutoff)).isTrue();
            assertThat(newer.isOlderThan(cutoff)).isFalse();
            assertThat(newer.isOlderThan(NOW)).isFalse();
        }
    }
}
