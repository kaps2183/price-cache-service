package com.example.pricecache.web;

import com.example.pricecache.service.StalePriceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static com.example.pricecache.support.PriceFixtures.NOW;
import static com.example.pricecache.support.PriceFixtures.price;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Direct unit tests of the handler; the HTTP wiring of the remaining handlers
 * (bean validation, malformed JSON) is covered by {@link PriceControllerTest}.
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Nested
    @DisplayName("Given a domain rule is violated")
    class GivenDomainViolation {

        @Test
        @DisplayName("when handled, then 400 Bad Request with the rule message is returned")
        void badRequest() {
            ResponseEntity<ApiError> response = handler.handleIllegalArgument(new IllegalArgumentException("bid must not exceed ask"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isEqualTo(
                    new ApiError(400, "Invalid price", java.util.List.of("bid must not exceed ask")));
        }
    }

    @Nested
    @DisplayName("Given a stale price is published")
    class GivenStalePrice {

        @Test
        @DisplayName("when handled, then 422 Unprocessable Entity is returned")
        void unprocessable() {
            StalePriceException ex = new StalePriceException(price("BLOOMBERG", "AAPL", NOW.minusSeconds(1)), NOW);

            ResponseEntity<ApiError> response = handler.handleStalePrice(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().details()).singleElement().asString().contains("BLOOMBERG");
        }
    }
}
