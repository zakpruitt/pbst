package com.zakpruitt.pbst.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PokemonCardSearchDTO {
    private String id;
    private String name;
    private String setName;
    private String setCode;
    private String cardNumber;
    private String imageUrl;
    private BigDecimal marketPrice;
}
