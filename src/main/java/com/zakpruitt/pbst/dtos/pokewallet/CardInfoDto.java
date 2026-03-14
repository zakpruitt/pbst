package com.zakpruitt.pbst.dtos.pokewallet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardInfoDto {
    private String name;
    private String set_name;
    private String set_code;
    private String card_number;
    private String rarity;
}