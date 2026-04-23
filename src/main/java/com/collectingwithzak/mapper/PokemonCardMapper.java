package com.collectingwithzak.mapper;

import com.collectingwithzak.dto.pokewallet.PokeWalletSearchResponse.PokeWalletCard;
import com.collectingwithzak.dto.pokewallet.PokeWalletSearchResponse.TcgPlayer;
import com.collectingwithzak.dto.response.CardSearchResult;
import com.collectingwithzak.dto.response.PokemonCardResponse;
import com.collectingwithzak.entity.PokemonCard;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PokemonCardMapper {

    PokemonCardResponse toResponse(PokemonCard entity);

    CardSearchResult toSearchResult(PokemonCard entity);

    List<CardSearchResult> toSearchResultList(List<PokemonCard> entities);

    @Mapping(source = "cardInfo.name", target = "name")
    @Mapping(source = "cardInfo.setCode", target = "setCode")
    @Mapping(source = "cardInfo.setName", target = "setName")
    @Mapping(source = "cardInfo.cardNumber", target = "cardNumber")
    @Mapping(source = "cardInfo.rarity", target = "rarity")
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
        if (card.getSetName() == null) card.setSetName("");
        if (card.getRarity() == null) card.setRarity("");

        TcgPlayer tcg = dto.getTcgplayer();
        if (tcg == null) return;

        if (tcg.getPrices() != null && !tcg.getPrices().isEmpty()) {
            var price = tcg.getPrices().getFirst();
            if (price.getMarketPrice() != null) card.setMarketPrice(price.getMarketPrice());
            if (price.getLowPrice() != null) card.setLowPrice(price.getLowPrice());
        }

        String url = tcg.getUrl();
        if (url == null || !url.contains("/product/")) {
            card.setImageUrl("");
            return;
        }
        String after = url.substring(url.indexOf("/product/") + "/product/".length());
        int q = after.indexOf('?');
        String productId = q >= 0 ? after.substring(0, q) : after;
        card.setImageUrl("https://tcgplayer-cdn.tcgplayer.com/product/" + productId + "_in_1000x1000.jpg");
    }
}
