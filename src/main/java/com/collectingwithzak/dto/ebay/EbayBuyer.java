package com.collectingwithzak.dto.ebay;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EbayBuyer {
    private String username;
}
