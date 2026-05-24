package com.zakpruitt.collectingwithzak.dto.ebay;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EbayAmount {
    private String value;
    private String currency;

    public double toDouble() {
        if (value == null || value.isBlank()) return 0;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
