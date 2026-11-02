package com.example.pricecache.web;

import com.example.pricecache.domain.Price;
import com.example.pricecache.service.PriceCacheService;
import com.example.pricecache.service.PublishResult;
import com.example.pricecache.service.StalePriceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static com.example.pricecache.support.PriceFixtures.NOW;
import static com.example.pricecache.support.PriceFixtures.price;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PriceController.class)
@DisplayName("PriceController")
class PriceControllerTest {

    private static final String VALID_REQUEST = """
            {
              "vendorId": "BLOOMBERG",
              "instrumentId": "AAPL",
              "bid": 189.10,
              "ask": 189.20,
              "currency": "USD",
              "timestamp": "2026-11-01T09:59:00Z"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PriceCacheService service;

    @Nested
    @DisplayName("Given a vendor publishes a price")
    class GivenVendorPublishes {

        @Test
        @DisplayName("when the price is valid and new, then 201 Created is returned and the domain price is passed on")
        void created() throws Exception {
            given(service.publish(any(Price.class))).willReturn(PublishResult.ACCEPTED);

            mockMvc.perform(post("/api/v1/prices").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.result").value("ACCEPTED"))
                    .andExpect(jsonPath("$.vendorId").value("BLOOMBERG"))
                    .andExpect(jsonPath("$.instrumentId").value("AAPL"));

            ArgumentCaptor<Price> captor = ArgumentCaptor.forClass(Price.class);
            then(service).should().publish(captor.capture());
            assertThat(captor.getValue())
                    .usingRecursiveComparison()
                    .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .isEqualTo(new Price("BLOOMBERG", "AAPL", new BigDecimal("189.10"), new BigDecimal("189.20"),
                            "USD", Instant.parse("2026-11-01T09:59:00Z")));
        }

        @Test
        @DisplayName("when the cache already holds a newer price, then 200 OK with IGNORED_OUT_OF_ORDER is returned")
        void ignored() throws Exception {
            given(service.publish(any(Price.class))).willReturn(PublishResult.IGNORED_OUT_OF_ORDER);

            mockMvc.perform(post("/api/v1/prices").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("IGNORED_OUT_OF_ORDER"));
        }

        @Test
        @DisplayName("when the price is older than the retention period, then 422 Unprocessable Entity is returned")
        void stale() throws Exception {
            given(service.publish(any(Price.class)))
                    .willThrow(new StalePriceException(price("BLOOMBERG", "AAPL", NOW), NOW));

            mockMvc.perform(post("/api/v1/prices").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status").value(422))
                    .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("retention")));
        }

        @Test
        @DisplayName("when mandatory fields are missing or invalid, then 400 Bad Request lists every violation")
        void invalidFields() throws Exception {
            String invalid = """
                    { "instrumentId": "AAPL", "bid": -1, "ask": 189.20, "currency": "usd", "timestamp": "2026-11-01T09:59:00Z" }
                    """;

            mockMvc.perform(post("/api/v1/prices").contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details", hasSize(3)))
                    .andExpect(jsonPath("$.details", hasItem(org.hamcrest.Matchers.startsWith("vendorId"))))
                    .andExpect(jsonPath("$.details", hasItem(org.hamcrest.Matchers.startsWith("bid"))))
                    .andExpect(jsonPath("$.details", hasItem(org.hamcrest.Matchers.startsWith("currency"))));
            then(service).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("when the bid exceeds the ask, then the domain rule is reported as 400 Bad Request")
        void crossedMarket() throws Exception {
            String crossed = VALID_REQUEST.replace("189.10", "190.00");

            mockMvc.perform(post("/api/v1/prices").contentType(MediaType.APPLICATION_JSON).content(crossed))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details[0]").value("bid must not exceed ask"));
            then(service).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("when the body is not valid JSON, then 400 Bad Request is returned")
        void malformed() throws Exception {
            mockMvc.perform(post("/api/v1/prices").contentType(MediaType.APPLICATION_JSON).content("{ not json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Malformed request body"));
        }
    }

    @Nested
    @DisplayName("Given prices are cached")
    class GivenCachedPrices {

        @Test
        @DisplayName("when a client asks for a vendor, then all of that vendor's prices are returned")
        void byVendor() throws Exception {
            given(service.getPricesByVendor("BLOOMBERG")).willReturn(List.of(
                    price("BLOOMBERG", "AAPL", "189.10", "189.20", NOW),
                    price("BLOOMBERG", "MSFT", "410.00", "410.50", NOW)));

            mockMvc.perform(get("/api/v1/prices/vendors/BLOOMBERG"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].instrumentId").value("AAPL"))
                    .andExpect(jsonPath("$[0].bid").value(189.10))
                    .andExpect(jsonPath("$[0].timestamp").value("2026-11-01T10:00:00Z"))
                    .andExpect(jsonPath("$[1].instrumentId").value("MSFT"));
        }

        @Test
        @DisplayName("when a client asks for an instrument, then every vendor's price for it is returned")
        void byInstrument() throws Exception {
            given(service.getPricesByInstrument("AAPL")).willReturn(List.of(
                    price("BLOOMBERG", "AAPL", NOW),
                    price("REUTERS", "AAPL", NOW)));

            mockMvc.perform(get("/api/v1/prices/instruments/AAPL"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].vendorId").value("BLOOMBERG"))
                    .andExpect(jsonPath("$[1].vendorId").value("REUTERS"));
        }

        @Test
        @DisplayName("when nothing matches, then an empty list is returned")
        void empty() throws Exception {
            given(service.getPricesByVendor("UNKNOWN")).willReturn(List.of());

            mockMvc.perform(get("/api/v1/prices/vendors/UNKNOWN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }
}
