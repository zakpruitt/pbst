package com.collectingwithzak.dto.ebay;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EbayOrder {
    private String orderId;
    private String creationDate;
    private String orderFulfillmentStatus;
    private EbayPricingSummary pricingSummary;
    private List<EbayLineItem> lineItems;
    private EbayBuyer buyer;
}
