package com.zakpruitt.collectingwithzak.config;

import com.zakpruitt.jbay.Jbay;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The one place the jbay client is constructed. eBay credentials are optional
 * (the app runs fine without them), so consumers must check {@link #isConfigured()}
 * before calling {@link #client()}.
 */
@Component
public class JbayProvider {

    private final Jbay jbay;

    public JbayProvider(@Value("${ebay.client-id}") String clientId,
                        @Value("${ebay.client-secret}") String clientSecret,
                        @Value("${ebay.refresh-token}") String refreshToken) {
        boolean configured = !clientId.isBlank() && !clientSecret.isBlank() && !refreshToken.isBlank();
        this.jbay = configured
                ? Jbay.builder().credentials(clientId, clientSecret).refreshToken(refreshToken).build()
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
