package com.zakpruitt.collectingwithzak.ebay;

/**
 * Thrown when an eBay-backed feature is used without eBay credentials configured.
 * Extends IllegalStateException so batch flows that already treat setup problems
 * as per-row failures keep working; unguarded paths are mapped to a friendly
 * error page by the GlobalExceptionHandler.
 */
public class EbayNotConfiguredException extends IllegalStateException {

    public EbayNotConfiguredException() {
        super("eBay credentials are not configured — set EBAY_CLIENT_ID, EBAY_CLIENT_SECRET, and EBAY_REFRESH_TOKEN");
    }
}
