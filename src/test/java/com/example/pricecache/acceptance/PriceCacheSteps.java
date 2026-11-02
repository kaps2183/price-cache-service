package com.example.pricecache.acceptance;

import com.example.pricecache.domain.Price;
import com.example.pricecache.domain.PriceKey;
import com.example.pricecache.repository.PriceRepository;
import com.example.pricecache.scheduling.StalePriceEvictionJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class PriceCacheSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PriceRepository repository;

    @Autowired
    private StalePriceEvictionJob evictionJob;

    @Autowired
    private RecordingPriceSubscriber downstream;

    private int lastPublishStatus;
    private JsonNode lastQueryResult;

    @Before
    public void resetState() {
        // The Spring context (and so the cache) is shared across scenarios; start each one empty.
        repository.deleteOlderThan(Instant.MAX);
        downstream.clear();
    }

    @Given("the following prices are published:")
    public void pricesArePublished(DataTable table) throws Exception {
        for (Map<String, String> row : table.asMaps()) {
            Instant timestamp = Instant.now().minus(Duration.ofMinutes(Long.parseLong(row.get("minutesAgo"))));
            publish(row.get("vendor"), row.get("instrument"), row.get("bid"), row.get("ask"), row.get("currency"), timestamp);
        }
    }

    @When("vendor {string} publishes a price for {string} that is {int} days old")
    public void vendorPublishesOldPrice(String vendor, String instrument, int days) throws Exception {
        publish(vendor, instrument, "10.00", "10.10", "USD", Instant.now().minus(Duration.ofDays(days)));
    }

    @Given("vendor {string} holds a cached price for {string} that is {int} days old")
    public void vendorHoldsOldPrice(String vendor, String instrument, int days) {
        // Bypasses the API (which rejects stale prices) to simulate a price that aged in the cache.
        Instant timestamp = Instant.now().minus(Duration.ofDays(days));
        repository.saveIfNewer(new Price(vendor, instrument, new BigDecimal("10.00"), new BigDecimal("10.10"), "USD", timestamp));
    }

    @When("the stale price eviction job runs")
    public void evictionRuns() {
        evictionJob.evictStalePrices();
    }

    @When("a client requests all prices from vendor {string}")
    public void requestByVendor(String vendor) throws Exception {
        lastQueryResult = query("/api/v1/prices/vendors/" + vendor);
    }

    @When("a client requests all prices for instrument {string}")
    public void requestByInstrument(String instrument) throws Exception {
        lastQueryResult = query("/api/v1/prices/instruments/" + instrument);
    }

    @Then("the client receives prices for instruments {string}")
    public void receivesInstruments(String csv) {
        assertThat(field(lastQueryResult, "instrumentId")).containsExactlyElementsOf(split(csv));
    }

    @Then("the client receives prices from vendors {string}")
    public void receivesVendors(String csv) {
        assertThat(field(lastQueryResult, "vendorId")).containsExactlyElementsOf(split(csv));
    }

    @Then("downstream subscribers received {int} price update(s)")
    public void downstreamReceived(int count) {
        assertThat(downstream.received()).hasSize(count);
    }

    @Then("the last publication returned status {int}")
    public void lastPublicationStatus(int status) {
        assertThat(lastPublishStatus).isEqualTo(status);
    }

    @Then("the cache no longer holds a price from {string} for {string}")
    public void cacheDoesNotHold(String vendor, String instrument) {
        assertThat(repository.findByKey(new PriceKey(vendor, instrument))).isEmpty();
    }

    @Then("the cache still holds a price from {string} for {string}")
    public void cacheHolds(String vendor, String instrument) {
        assertThat(repository.findByKey(new PriceKey(vendor, instrument))).isPresent();
    }

    @Then("the cached {string} bid for {string} is {bigdecimal}")
    public void cachedBid(String vendor, String instrument, BigDecimal bid) throws Exception {
        JsonNode prices = query("/api/v1/prices/instruments/" + instrument);
        JsonNode match = null;
        for (JsonNode price : prices) {
            if (vendor.equals(price.get("vendorId").asText())) {
                match = price;
            }
        }
        assertThat(match).as("price from %s for %s", vendor, instrument).isNotNull();
        assertThat(match.get("bid").decimalValue()).isEqualByComparingTo(bid);
    }

    private void publish(String vendor, String instrument, String bid, String ask, String currency, Instant timestamp)
            throws Exception {
        String body = """
                {"vendorId":"%s","instrumentId":"%s","bid":%s,"ask":%s,"currency":"%s","timestamp":"%s"}
                """.formatted(vendor, instrument, bid, ask, currency, timestamp);
        MvcResult result = mockMvc.perform(post("/api/v1/prices").contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn();
        lastPublishStatus = result.getResponse().getStatus();
    }

    private JsonNode query(String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path)).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static List<String> field(JsonNode array, String name) {
        List<String> values = new ArrayList<>();
        array.forEach(node -> values.add(node.get(name).asText()));
        return values;
    }

    private static List<String> split(String csv) {
        return Arrays.stream(csv.split(",")).map(String::trim).toList();
    }
}
