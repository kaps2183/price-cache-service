package com.example.pricecache.web;

import com.example.pricecache.domain.Price;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.example.pricecache.support.PriceFixtures.NOW;
import static com.example.pricecache.support.PriceFixtures.price;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PriceResponse")
class PriceResponseTest {

    @Test
    @DisplayName("Given a domain price, when mapped, then every field is carried over")
    void fromDomain() {
        Price price = price("BLOOMBERG", "AAPL", "189.10", "189.20", NOW);

        PriceResponse response = PriceResponse.from(price);

        assertThat(response.vendorId()).isEqualTo("BLOOMBERG");
        assertThat(response.instrumentId()).isEqualTo("AAPL");
        assertThat(response.bid()).isEqualByComparingTo("189.10");
        assertThat(response.ask()).isEqualByComparingTo("189.20");
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.timestamp()).isEqualTo(NOW);
    }
}
