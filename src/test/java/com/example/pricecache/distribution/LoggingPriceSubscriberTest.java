package com.example.pricecache.distribution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static com.example.pricecache.support.PriceFixtures.NOW;
import static com.example.pricecache.support.PriceFixtures.price;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
@DisplayName("LoggingPriceSubscriber")
class LoggingPriceSubscriberTest {

    private final LoggingPriceSubscriber subscriber = new LoggingPriceSubscriber();

    @Test
    @DisplayName("Given a published price, when it is received, then it is written to the log")
    void logsPrice(CapturedOutput output) {
        subscriber.onPrice(price("BLOOMBERG", "AAPL", NOW));

        assertThat(output).contains("BLOOMBERG").contains("AAPL").contains("100.00");
    }

    @Test
    @DisplayName("Given the subscriber, when asked for its name, then it identifies itself")
    void name() {
        assertThat(subscriber.name()).isEqualTo("logging");
    }
}
