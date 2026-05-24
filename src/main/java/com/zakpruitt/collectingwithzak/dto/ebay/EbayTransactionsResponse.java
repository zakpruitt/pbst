package com.zakpruitt.collectingwithzak.dto.ebay;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EbayTransactionsResponse {
    private List<EbayTransaction> transactions;
}
