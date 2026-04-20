package com.collectingwithzak.mapper;

import com.collectingwithzak.dto.pokewallet.PokeWalletSearchResponse.PokeWalletCard;
import com.collectingwithzak.dto.pokewallet.PokeWalletSearchResponse.TcgPlayer;
import com.collectingwithzak.entity.PokemonCard;
import org.mapstruct.*;

import java.time.LocalDateTime;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PokemonCardMapper {

    @Mapping(source = "card_info.name", target = "name")
    @Mapping(source = "card_info.set_code", target = "setCode")
    @Mapping(source = "card_info.set_name", target = "setName")
    @Mapping(source = "card_info.card_number", target = "cardNumber")
    @Mapping(source = "card_info.rarity", target = "rarity")
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "marketPrice", ignore = true)
    @Mapping(target = "lowPrice", ignore = true)
    @Mapping(target = "lastPriceSync", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PokemonCard fromPokeWallet(PokeWalletCard dto);

    @AfterMapping
    default void mapPricesAndImage(PokeWalletCard dto, @MappingTarget PokemonCard card) {
        card.setLastPriceSync(LocalDateTime.now());

        TcgPlayer tcg = dto.getTcgplayer();
        if (tcg == null) return;

        if (tcg.getPrices() != null && !tcg.getPrices().isEmpty()) {
            var price = tcg.getPrices().getFirst();
            if (price.getMarket_price() != null) card.setMarketPrice(price.getMarket_price());
            if (price.getLow_price() != null) card.setLowPrice(price.getLow_price());
        }
        card.setImageUrl(tcgImageUrl(tcg.getUrl()));
    }

    default String tcgImageUrl(String url) {
        if (url == null || !url.contains("/product/")) return "";
        String after = url.substring(url.indexOf("/product/") + "/product/".length());
        int q = after.indexOf('?');
        String productId = q >= 0 ? after.substring(0, q) : after;
        return "https://tcgplayer-cdn.tcgplayer.com/product/" + productId + "_in_1000x1000.jpg";
    }
}
