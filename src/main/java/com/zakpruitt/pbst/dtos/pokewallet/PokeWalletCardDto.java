package com.zakpruitt.pbst.dtos.pokewallet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PokeWalletCardDto {
    private String id;
    private CardInfoDto card_info;
    private TcgPlayerDto tcgplayer;
}