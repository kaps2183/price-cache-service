package com.example.pricecache.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClockConfig")
class ClockConfigTest {

    @Test
    @DisplayName("Given the application starts, when the clock bean is created, then it is a UTC system clock")
    void utcClock() {
        assertThat(new ClockConfig().clock().getZone()).isEqualTo(ZoneOffset.UTC);
    }
}
