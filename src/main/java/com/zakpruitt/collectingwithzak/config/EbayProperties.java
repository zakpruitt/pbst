package com.zakpruitt.collectingwithzak.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** eBay keyset credentials. All blank when the app runs without eBay integration. */
@ConfigurationProperties(prefix = "ebay")
public record EbayProperties(String clientId, String clientSecret, String refreshToken) {

    public boolean isConfigured() {
        return !clientId.isBlank() && !clientSecret.isBlank() && !refreshToken.isBlank();
    }
}
