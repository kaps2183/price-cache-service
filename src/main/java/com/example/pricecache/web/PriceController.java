package com.example.pricecache.web;

import com.example.pricecache.domain.Price;
import com.example.pricecache.service.PriceCacheService;
import com.example.pricecache.service.PublishResult;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API for vendors to publish prices and for clients to retrieve them.
 */
@RestController
@RequestMapping("/api/v1/prices")
public class PriceController {

    private static final Logger log = LoggerFactory.getLogger(PriceController.class);

    private final PriceCacheService service;

    public PriceController(PriceCacheService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PublishPriceResponse> publish(@Valid @RequestBody PublishPriceRequest request) {
        log.debug("POST price {}/{} at {}", request.vendorId(), request.instrumentId(), request.timestamp());
        Price price = request.toDomain();
        PublishResult result = service.publish(price);
        HttpStatus status = result == PublishResult.ACCEPTED ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status)
                .body(new PublishPriceResponse(result, price.vendorId(), price.instrumentId()));
    }

    @GetMapping("/vendors/{vendorId}")
    public List<PriceResponse> getByVendor(@PathVariable String vendorId) {
        log.debug("GET prices for vendor {}", vendorId);
        return service.getPricesByVendor(vendorId).stream().map(PriceResponse::from).toList();
    }

    @GetMapping("/instruments/{instrumentId}")
    public List<PriceResponse> getByInstrument(@PathVariable String instrumentId) {
        log.debug("GET prices for instrument {}", instrumentId);
        return service.getPricesByInstrument(instrumentId).stream().map(PriceResponse::from).toList();
    }
}
