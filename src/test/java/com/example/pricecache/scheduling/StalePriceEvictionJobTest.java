package com.example.pricecache.scheduling;

import com.example.pricecache.service.PriceCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("StalePriceEvictionJob")
class StalePriceEvictionJobTest {

    @Mock
    private PriceCacheService service;

    @InjectMocks
    private StalePriceEvictionJob job;

    @Nested
    @DisplayName("Given the scheduler fires")
    class GivenSchedulerFires {

        @Test
        @DisplayName("when the job runs, then it delegates eviction to the service")
        void delegates() {
            given(service.evictStalePrices()).willReturn(2);

            job.evictStalePrices();

            then(service).should().evictStalePrices();
        }

        @Test
        @DisplayName("when the eviction fails, then the error is logged and not propagated to the scheduler")
        void swallowsFailure() {
            given(service.evictStalePrices()).willThrow(new IllegalStateException("store unavailable"));

            assertThatCode(job::evictStalePrices).doesNotThrowAnyException();
        }
    }
}
