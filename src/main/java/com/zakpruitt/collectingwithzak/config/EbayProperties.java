package com.zakpruitt.collectingwithzak.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * eBay keyset credentials. All blank when the app runs without eBay integration.
 */
@ConfigurationProperties(prefix = "ebay")
public record EbayProperties(String clientId, String clientSecret, String refreshToken) {

    public boolean isConfigured() {
        return StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret) && StringUtils.hasText(refreshToken);
    }
}
