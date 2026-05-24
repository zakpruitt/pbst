package com.zakpruitt.collectingwithzak.dto.ebay;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EbayLineItem {
    private String title;
}
