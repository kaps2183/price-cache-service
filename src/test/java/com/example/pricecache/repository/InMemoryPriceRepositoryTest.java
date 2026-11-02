package com.example.pricecache.repository;

import com.example.pricecache.domain.Price;
import com.example.pricecache.domain.PriceKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.example.pricecache.support.PriceFixtures.NOW;
import static com.example.pricecache.support.PriceFixtures.price;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InMemoryPriceRepository")
class InMemoryPriceRepositoryTest {

    private InMemoryPriceRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPriceRepository();
    }

    @Nested
    @DisplayName("Given an empty repository")
    class GivenEmptyRepository {

        @Test
        @DisplayName("when a price is saved, then it is stored and retrievable by key, vendor and instrument")
        void saveNewPrice() {
            Price price = price("BLOOMBERG", "AAPL", NOW);

            boolean saved = repository.saveIfNewer(price);

            assertThat(saved).isTrue();
            assertThat(repository.findByKey(price.key())).contains(price);
            assertThat(repository.findByVendor("BLOOMBERG")).containsExactly(price);
            assertThat(repository.findByInstrument("AAPL")).containsExactly(price);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("when an unknown vendor or instrument is queried, then nothing is returned")
        void unknownLookups() {
            assertThat(repository.findByVendor("UNKNOWN")).isEmpty();
            assertThat(repository.findByInstrument("UNKNOWN")).isEmpty();
            assertThat(repository.findByKey(new PriceKey("UNKNOWN", "UNKNOWN"))).isEmpty();
        }

        @Test
        @DisplayName("when eviction runs, then nothing is removed")
        void evictEmpty() {
            assertThat(repository.deleteOlderThan(NOW)).isZero();
        }
    }

    @Nested
    @DisplayName("Given a price already cached for a vendor and instrument")
    class GivenExistingPrice {

        private final Price existing = price("BLOOMBERG", "AAPL", "100.00", "100.10", NOW);

        @BeforeEach
        void cacheExisting() {
            repository.saveIfNewer(existing);
        }

        @Test
        @DisplayName("when a newer price arrives, then it replaces the old one in every index")
        void newerReplaces() {
            Price newer = price("BLOOMBERG", "AAPL", "101.00", "101.10", NOW.plusSeconds(1));

            boolean saved = repository.saveIfNewer(newer);

            assertThat(saved).isTrue();
            assertThat(repository.findByVendor("BLOOMBERG")).containsExactly(newer);
            assertThat(repository.findByInstrument("AAPL")).containsExactly(newer);
            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("when an older (out-of-order) price arrives, then it is ignored")
        void olderIgnored() {
            Price older = price("BLOOMBERG", "AAPL", "99.00", "99.10", NOW.minusSeconds(1));

            boolean saved = repository.saveIfNewer(older);

            assertThat(saved).isFalse();
            assertThat(repository.findByKey(existing.key())).contains(existing);
        }

        @Test
        @DisplayName("when a price with the same timestamp arrives, then it is ignored as a duplicate")
        void duplicateIgnored() {
            Price duplicate = price("BLOOMBERG", "AAPL", "99.00", "99.10", NOW);

            assertThat(repository.saveIfNewer(duplicate)).isFalse();
            assertThat(repository.findByKey(existing.key())).contains(existing);
        }
    }

    @Nested
    @DisplayName("Given prices from several vendors for several instruments")
    class GivenSeveralPrices {

        private final Price bloombergAapl = price("BLOOMBERG", "AAPL", NOW);
        private final Price bloombergMsft = price("BLOOMBERG", "MSFT", NOW);
        private final Price reutersAapl = price("REUTERS", "AAPL", NOW);
        private final Price reutersVod = price("REUTERS", "VOD.L", NOW.minus(Duration.ofDays(31)));

        @BeforeEach
        void cacheAll() {
            List.of(bloombergMsft, reutersAapl, bloombergAapl, reutersVod).forEach(repository::saveIfNewer);
        }

        @Test
        @DisplayName("when queried by vendor, then only that vendor's prices are returned, ordered by instrument")
        void byVendor() {
            assertThat(repository.findByVendor("BLOOMBERG")).containsExactly(bloombergAapl, bloombergMsft);
        }

        @Test
        @DisplayName("when queried by instrument, then every vendor's price is returned, ordered by vendor")
        void byInstrument() {
            assertThat(repository.findByInstrument("AAPL")).containsExactly(bloombergAapl, reutersAapl);
        }

        @Test
        @DisplayName("when prices older than a cutoff are deleted, then they disappear from every index")
        void deleteOlderThan() {
            int removed = repository.deleteOlderThan(NOW.minus(Duration.ofDays(30)));

            assertThat(removed).isEqualTo(1);
            assertThat(repository.count()).isEqualTo(3);
            assertThat(repository.findByVendor("REUTERS")).containsExactly(reutersAapl);
            assertThat(repository.findByInstrument("VOD.L")).isEmpty();
        }

        @Test
        @DisplayName("when everything is older than the cutoff, then the repository is emptied")
        void deleteAll() {
            int removed = repository.deleteOlderThan(Instant.MAX);

            assertThat(removed).isEqualTo(4);
            assertThat(repository.count()).isZero();
            assertThat(repository.findByVendor("BLOOMBERG")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Given many threads publishing the same instrument concurrently")
    class GivenConcurrentPublishers {

        @Test
        @DisplayName("when all updates are applied, then the newest price wins and the indexes stay consistent")
        void newestWins() throws Exception {
            int updates = 1_000;
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int i = 0; i < updates; i++) {
                Price p = price("BLOOMBERG", "AAPL", NOW.plusMillis(i));
                tasks.add(() -> repository.saveIfNewer(p));
            }

            runConcurrently(tasks);

            Instant newest = NOW.plusMillis(updates - 1);
            assertThat(repository.count()).isEqualTo(1);
            assertThat(repository.findByVendor("BLOOMBERG")).singleElement()
                    .extracting(Price::timestamp).isEqualTo(newest);
            assertThat(repository.findByInstrument("AAPL")).singleElement()
                    .extracting(Price::timestamp).isEqualTo(newest);
        }

        @Test
        @DisplayName("when eviction runs while fresh prices replace stale ones, then no fresh price is lost from either index")
        void evictionDoesNotDropFreshPrices() throws Exception {
            int instruments = 500;
            Instant stale = NOW.minus(Duration.ofDays(31));
            Instant cutoff = NOW.minus(Duration.ofDays(30));
            for (int i = 0; i < instruments; i++) {
                repository.saveIfNewer(price("BLOOMBERG", "INST-" + i, stale));
            }

            List<Callable<Object>> tasks = new ArrayList<>();
            for (int i = 0; i < instruments; i++) {
                Price fresh = price("BLOOMBERG", "INST-" + i, NOW);
                tasks.add(() -> repository.saveIfNewer(fresh));
                if (i % 50 == 0) {
                    tasks.add(() -> repository.deleteOlderThan(cutoff));
                }
            }

            runConcurrently(tasks);

            assertThat(repository.count()).isEqualTo(instruments);
            assertThat(repository.findByVendor("BLOOMBERG"))
                    .hasSize(instruments)
                    .allSatisfy(p -> assertThat(p.timestamp()).isEqualTo(NOW));
            for (int i = 0; i < instruments; i++) {
                assertThat(repository.findByInstrument("INST-" + i)).singleElement()
                        .extracting(Price::timestamp).isEqualTo(NOW);
            }
        }

        private <T> void runConcurrently(List<Callable<T>> tasks) throws InterruptedException {
            ExecutorService pool = Executors.newFixedThreadPool(8);
            try {
                pool.invokeAll(tasks);
            } finally {
                pool.shutdown();
                assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
            }
        }
    }
}
