package com.example.pricecache;

import com.example.pricecache.config.PriceCacheProperties;
import com.example.pricecache.repository.InMemoryPriceRepository;
import com.example.pricecache.repository.PriceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("PriceCacheApplication")
class PriceCacheApplicationTest {

    @Autowired
    private PriceRepository repository;

    @Autowired
    private PriceCacheProperties properties;

    @Test
    @DisplayName("Given the default configuration, when the context starts, then the in-memory store and 30-day retention are wired")
    void contextLoads() {
        assertThat(repository).isInstanceOf(InMemoryPriceRepository.class);
        assertThat(properties.retention()).isEqualTo(Duration.ofDays(30));
        assertThat(properties.evictionInterval()).isEqualTo(Duration.ofHours(1));
    }
}
