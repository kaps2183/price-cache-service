package com.example.pricecache.web;

import java.util.List;

/**
 * Uniform error body returned by the API.
 */
public record ApiError(int status, String error, List<String> details) {
}
