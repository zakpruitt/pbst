package com.collectingwithzak.dto.pokewallet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
        @JsonProperty("total_pages")
        private int totalPages;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PokeWalletCard {
        private String id;
        @JsonProperty("card_info")
        private CardInfo cardInfo;
        private TcgPlayer tcgplayer;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CardInfo {
        private String name;
        @JsonProperty("set_name")
        private String setName;
        @JsonProperty("set_code")
        private String setCode;
        @JsonProperty("card_number")
        private String cardNumber;
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
        @JsonProperty("market_price")
        private Double marketPrice;
        @JsonProperty("low_price")
        private Double lowPrice;
    }
}
