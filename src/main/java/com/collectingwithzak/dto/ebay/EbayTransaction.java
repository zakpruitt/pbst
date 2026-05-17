package com.collectingwithzak.dto.ebay;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EbayTransaction {
    private String orderId;
    private String transactionType;
    private EbayAmount totalFeeAmount;
    private EbayAmount amount;
}
