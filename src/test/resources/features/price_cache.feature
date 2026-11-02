Feature: Vendor price cache
  As a downstream system
  I want prices from many vendors cached in one place
  So that I can read all prices from a vendor, or every vendor's price for an instrument

  Background:
    Given the following prices are published:
      | vendor    | instrument | bid    | ask    | currency | minutesAgo |
      | BLOOMBERG | AAPL       | 189.10 | 189.20 | USD      | 1          |
      | BLOOMBERG | MSFT       | 410.00 | 410.50 | USD      | 1          |
      | REUTERS   | AAPL       | 189.05 | 189.25 | USD      | 2          |
      | ICE       | VOD.L      | 72.10  | 72.20  | GBP      | 3          |

  Scenario: Retrieve all prices from a particular vendor
    When a client requests all prices from vendor "BLOOMBERG"
    Then the client receives prices for instruments "AAPL,MSFT"

  Scenario: Retrieve prices for a single instrument from various vendors
    When a client requests all prices for instrument "AAPL"
    Then the client receives prices from vendors "BLOOMBERG,REUTERS"

  Scenario: Published prices are distributed to downstream systems
    Then downstream subscribers received 4 price updates

  Scenario: An out-of-order update does not overwrite a newer price
    When the following prices are published:
      | vendor    | instrument | bid    | ask    | currency | minutesAgo |
      | BLOOMBERG | AAPL       | 150.00 | 150.10 | USD      | 10         |
    Then the last publication returned status 200
    And the cached "BLOOMBERG" bid for "AAPL" is 189.10

  Scenario: A price older than 30 days is rejected on publish
    When vendor "REUTERS" publishes a price for "MSFT" that is 31 days old
    Then the last publication returned status 422

  Scenario: Prices older than 30 days are removed from the cache
    Given vendor "ICE" holds a cached price for "MSFT" that is 31 days old
    When the stale price eviction job runs
    Then the cache no longer holds a price from "ICE" for "MSFT"
    And the cache still holds a price from "ICE" for "VOD.L"

  Scenario: A stale price awaiting eviction is never served to clients
    Given vendor "ICE" holds a cached price for "MSFT" that is 31 days old
    When a client requests all prices from vendor "ICE"
    Then the client receives prices for instruments "VOD.L"
