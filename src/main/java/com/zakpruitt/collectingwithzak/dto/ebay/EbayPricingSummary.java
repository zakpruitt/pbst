package com.zakpruitt.collectingwithzak.dto.ebay;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EbayPricingSummary {
    private EbayAmount priceSubtotal;
    private EbayAmount deliveryCost;
}
