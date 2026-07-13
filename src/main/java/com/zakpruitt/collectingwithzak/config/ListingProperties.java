package com.zakpruitt.collectingwithzak.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tuning for the eBay listings automation.
 *
 * @param undercutPercent how far under the lowest comp to price, in percent
 * @param floorPercent    lowest acceptable price as a percent of the card's market price,
 *                        so one junk comp can't tank the suggestion
 * @param categoryId      eBay category for created listings (183454 = CCG Individual Cards)
 * @param marketplace     eBay marketplace id, e.g. EBAY_US
 * @param compLimit       how many comps to fetch per card
 * @param excludeSeller   own eBay username, so own listings are not treated as comps
 */
@ConfigurationProperties(prefix = "listing")
public record ListingProperties(double undercutPercent, double floorPercent, String categoryId,
                                String marketplace, int compLimit, String excludeSeller) {
}
