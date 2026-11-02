#!/usr/bin/env bash
# Walks through the API against a running instance:
#   mvn spring-boot:run -Dspring-boot.run.profiles=demo
#   ./demo/demo.sh
set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}/api/v1/prices"
NOW=$(date -u +%Y-%m-%dT%H:%M:%SZ)
EARLIER=$(date -u -v-10M +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -d '10 minutes ago' +%Y-%m-%dT%H:%M:%SZ)
STALE=$(date -u -v-31d +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -d '31 days ago' +%Y-%m-%dT%H:%M:%SZ)

step() { printf '\n\033[1m== %s\033[0m\n' "$1"; }
call() { curl -s -w '\n-> HTTP %{http_code}\n' "$@"; }

step "1. Vendor BLOOMBERG publishes a new AAPL price (expect 201 ACCEPTED)"
call -X POST "$BASE" -H 'Content-Type: application/json' \
  -d "{\"vendorId\":\"BLOOMBERG\",\"instrumentId\":\"AAPL\",\"bid\":190.00,\"ask\":190.10,\"currency\":\"USD\",\"timestamp\":\"$NOW\"}"

step "2. A delayed, older BLOOMBERG AAPL price arrives (expect 200 IGNORED_OUT_OF_ORDER)"
call -X POST "$BASE" -H 'Content-Type: application/json' \
  -d "{\"vendorId\":\"BLOOMBERG\",\"instrumentId\":\"AAPL\",\"bid\":150.00,\"ask\":150.10,\"currency\":\"USD\",\"timestamp\":\"$EARLIER\"}"

step "3. A price older than 30 days is published (expect 422)"
call -X POST "$BASE" -H 'Content-Type: application/json' \
  -d "{\"vendorId\":\"REUTERS\",\"instrumentId\":\"AAPL\",\"bid\":100.00,\"ask\":100.10,\"currency\":\"USD\",\"timestamp\":\"$STALE\"}"

step "4. Invalid payload: bid above ask (expect 400)"
call -X POST "$BASE" -H 'Content-Type: application/json' \
  -d "{\"vendorId\":\"REUTERS\",\"instrumentId\":\"AAPL\",\"bid\":200.00,\"ask\":100.00,\"currency\":\"USD\",\"timestamp\":\"$NOW\"}"

step "5. All prices from vendor BLOOMBERG"
call "$BASE/vendors/BLOOMBERG"

step "6. AAPL prices from every vendor"
call "$BASE/instruments/AAPL"
