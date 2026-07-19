package com.zakpruitt.collectingwithzak.ebay;

import com.zakpruitt.jbay.Jbay;
import org.springframework.stereotype.Component;

/**
 * The one place the jbay client is constructed. eBay credentials are optional
 * (the app runs fine without them); {@link #client()} throws
 * {@link EbayNotConfiguredException} when they are missing, which the
 * GlobalExceptionHandler turns into a friendly error page.
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
            throw new EbayNotConfiguredException();
        }
        return jbay;
    }
}
