package com.example.pricecache.web;

import com.example.pricecache.service.PublishResult;

public record PublishPriceResponse(PublishResult result, String vendorId, String instrumentId) {
}
