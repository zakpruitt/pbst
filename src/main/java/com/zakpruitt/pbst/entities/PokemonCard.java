package com.zakpruitt.pbst.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PokemonCard {

    @Id
    private String id;

    private String name;
    private String setCode;
    private String setName;
    private String cardNumber;
    private String rarity;
    private String imageUrl;

    private BigDecimal marketPrice;
    private BigDecimal lowPrice;

    private LocalDateTime lastPriceSync;
}