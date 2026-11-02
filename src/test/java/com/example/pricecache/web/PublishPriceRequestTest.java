package com.example.pricecache.web;

import com.example.pricecache.domain.Price;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static com.example.pricecache.support.PriceFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PublishPriceRequest")
class PublishPriceRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Nested
    @DisplayName("Given a complete request")
    class GivenCompleteRequest {

        private final PublishPriceRequest request = new PublishPriceRequest(
                "BLOOMBERG", "AAPL", new BigDecimal("189.10"), new BigDecimal("189.20"), "USD", NOW);

        @Test
        @DisplayName("when validated, then there are no violations")
        void valid() {
            assertThat(validator.validate(request)).isEmpty();
        }

        @Test
        @DisplayName("when converted, then a domain price with the same values is produced")
        void toDomain() {
            Price price = request.toDomain();

            assertThat(price).isEqualTo(new Price("BLOOMBERG", "AAPL", new BigDecimal("189.10"),
                    new BigDecimal("189.20"), "USD", NOW));
        }
    }

    @Nested
    @DisplayName("Given an incomplete request")
    class GivenIncompleteRequest {

        @Test
        @DisplayName("when validated, then each missing or malformed field is reported")
        void invalid() {
            PublishPriceRequest request = new PublishPriceRequest(" ", null, BigDecimal.ZERO, null, "US", null);

            Set<ConstraintViolation<PublishPriceRequest>> violations = validator.validate(request);

            assertThat(violations)
                    .extracting(v -> v.getPropertyPath().toString())
                    .containsExactlyInAnyOrder("vendorId", "instrumentId", "bid", "ask", "currency", "timestamp");
        }
    }
}
