package com.example.pricecache.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@DisplayName("PriceKey")
class PriceKeyTest {

    @Nested
    @DisplayName("Given a vendor and an instrument")
    class GivenVendorAndInstrument {

        @Test
        @DisplayName("when two keys are built from the same ids, then they are equal")
        void equalKeys() {
            PriceKey first = new PriceKey("BLOOMBERG", "AAPL");
            PriceKey second = new PriceKey("BLOOMBERG", "AAPL");

            assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
        }

        @Test
        @DisplayName("when the vendor differs, then the keys are not equal")
        void differentVendor() {
            assertThat(new PriceKey("BLOOMBERG", "AAPL")).isNotEqualTo(new PriceKey("REUTERS", "AAPL"));
        }
    }

    @Nested
    @DisplayName("Given a missing id")
    class GivenMissingId {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("when the vendor id is blank, then construction is rejected")
        void blankVendor(String vendorId) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new PriceKey(vendorId, "AAPL"))
                    .withMessageContaining("vendorId");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = "   ")
        @DisplayName("when the instrument id is blank, then construction is rejected")
        void blankInstrument(String instrumentId) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new PriceKey("BLOOMBERG", instrumentId))
                    .withMessageContaining("instrumentId");
        }
    }
}
