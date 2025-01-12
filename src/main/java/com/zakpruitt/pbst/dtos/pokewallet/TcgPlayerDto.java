package com.zakpruitt.pbst.dtos.pokewallet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TcgPlayerDto {
    private String url;
    private List<PriceVariantDto> prices;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PriceVariantDto {
        private BigDecimal market_price;
        private BigDecimal low_price;
        private String sub_type_name; // e.g., "Normal", "Holofoil"
    }
}