package com.zakpruitt.collectingwithzak.dto.pokewallet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PokeWalletSetsResponse {

    private List<PokeWalletSet> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PokeWalletSet {
        private String name;
        @JsonProperty("set_code")
        private String setCode;
        @JsonProperty("set_id")
        private String setId;
        @JsonProperty("card_count")
        private int cardCount;
        private String language;
    }
}
