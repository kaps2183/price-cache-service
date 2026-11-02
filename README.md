# Price Cache Service

A Spring Boot service that caches instrument prices from multiple vendors, distributes each new price to downstream systems, and serves two queries:

- all prices from a **vendor**
- one **instrument**'s prices from every vendor

Prices older than **30 days** are rejected on publish, hidden from reads, and purged by a scheduled job.

Full design (sequence diagrams, domain model, patterns, problems and solutions): **[docs/DESIGN.md](docs/DESIGN.md)**.

## Tech stack

Java 21 · Spring Boot 3.5 · Maven · JUnit 5 · AssertJ · BDDMockito · Cucumber 7

## Build and test

```bash
mvn clean verify
```

This runs the unit, slice and Cucumber acceptance tests.

## Run

```bash
mvn spring-boot:run
```

With sample data from 3 vendors for 3 instruments:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

## API

| Method | Path | Description | Responses |
|---|---|---|---|
| `POST` | `/api/v1/prices` | Publish a vendor price | `201` stored · `200` ignored (older than cached) · `400` invalid · `422` older than 30 days |
| `GET` | `/api/v1/prices/vendors/{vendorId}` | All prices from a vendor | `200` |
| `GET` | `/api/v1/prices/instruments/{instrumentId}` | One instrument's prices from every vendor | `200` |

Example request:

```json
{
  "vendorId": "BLOOMBERG",
  "instrumentId": "AAPL",
  "bid": 189.10,
  "ask": 189.20,
  "currency": "USD",
  "timestamp": "2026-11-01T09:59:00Z"
}
```

## Demo

With the app running (demo profile), run the scripted walkthrough:

```bash
./demo/demo.sh
```

You can also use [demo/price-cache.http](demo/price-cache.http) from the IntelliJ or VS Code REST client.

## Configuration

| Property | Default | Meaning |
|---|---|---|
| `price-cache.retention` | `30d` | Maximum age of a cached price |
| `price-cache.eviction-interval` | `PT1H` | How often stale prices are purged |

## Project layout

```text
src/main/java/com/example/pricecache
├── domain/         Price, PriceKey (immutable records with invariants)
├── repository/     PriceRepository port, InMemoryPriceRepository adapter
├── service/        PriceCacheService, events, results, exceptions
├── distribution/   PriceSubscriber, DownstreamPriceDistributor, LoggingPriceSubscriber
├── scheduling/     StalePriceEvictionJob
├── web/            PriceController, DTOs, GlobalExceptionHandler
├── config/         PriceCacheProperties, ClockConfig
└── demo/           DemoDataLoader (profile "demo")
src/test/java/...   One BDD-style test class per production class, plus Cucumber steps
src/test/resources/features/price_cache.feature
```
