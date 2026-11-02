package com.example.pricecache.acceptance;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Boots the full application once for all scenarios and registers a recording
 * downstream subscriber so scenarios can assert on distribution.
 */
@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
@Import(CucumberSpringConfiguration.RecordingSubscriberConfig.class)
public class CucumberSpringConfiguration {

    @TestConfiguration
    static class RecordingSubscriberConfig {

        @Bean
        RecordingPriceSubscriber recordingPriceSubscriber() {
            return new RecordingPriceSubscriber();
        }
    }
}
