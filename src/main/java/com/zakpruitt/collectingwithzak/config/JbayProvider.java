package com.zakpruitt.collectingwithzak.config;

import com.zakpruitt.jbay.Jbay;
import org.springframework.stereotype.Component;

/**
 * The one place the jbay client is constructed. eBay credentials are optional
 * (the app runs fine without them), so consumers must check {@link #isConfigured()}
 * before calling {@link #client()}.
 */
@Component
public class JbayProvider {

    private final Jbay jbay;

    public JbayProvider(EbayProperties properties) {
        this.jbay = properties.isConfigured()
                ? Jbay.builder()
                        .credentials(properties.clientId(), properties.clientSecret())
                        .refreshToken(properties.refreshToken())
                        .build()
                : null;
    }

    public boolean isConfigured() {
        return jbay != null;
    }

    public Jbay client() {
        if (jbay == null) {
            throw new IllegalStateException("eBay credentials are not configured");
        }
        return jbay;
    }
}
