package com.collectingwithzak.dto.pokewallet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PokeWalletSearchResponse {
    private List<PokeWalletCard> results;
    private Pagination pagination;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pagination {
        private int page;
        private int total_pages;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PokeWalletCard {
        private String id;
        private CardInfo card_info;
        private TcgPlayer tcgplayer;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CardInfo {
        private String name;
        private String set_name;
        private String set_code;
        private String card_number;
        private String rarity;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TcgPlayer {
        private String url;
        private List<PriceVariant> prices;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PriceVariant {
        private Double market_price;
        private Double low_price;
    }
}
