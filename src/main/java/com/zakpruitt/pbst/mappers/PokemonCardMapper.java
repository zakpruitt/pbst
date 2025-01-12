package com.zakpruitt.pbst.mappers;

import com.zakpruitt.pbst.dtos.pokewallet.PokeWalletCardDto;
import com.zakpruitt.pbst.dtos.pokewallet.TcgPlayerDto;
import com.zakpruitt.pbst.entities.PokemonCard;
import org.mapstruct.*;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface PokemonCardMapper {

    @Mapping(target = "name", source = "card_info.name")
    @Mapping(target = "setCode", source = "card_info.set_code")
    @Mapping(target = "setName", source = "card_info.set_name")
    @Mapping(target = "cardNumber", source = "card_info.card_number")
    @Mapping(target = "rarity", source = "card_info.rarity")
    @Mapping(target = "marketPrice", ignore = true)
    @Mapping(target = "lowPrice", ignore = true)
    @Mapping(target = "lastPriceSync", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    PokemonCard toEntity(PokeWalletCardDto dto);

    @AfterMapping
    default void enrichEntity(PokeWalletCardDto dto, @MappingTarget PokemonCard entity) {
        entity.setLastPriceSync(LocalDateTime.now());

        if (dto.getTcgplayer() != null) {
            updatePrices(dto.getTcgplayer(), entity);
            updateImageUrl(dto.getTcgplayer(), entity);
        }
    }

    default void updatePrices(TcgPlayerDto tcgData, PokemonCard entity) {
        if (tcgData.getPrices() != null && !tcgData.getPrices().isEmpty()) {
            TcgPlayerDto.PriceVariantDto priceData = tcgData.getPrices().get(0);
            entity.setMarketPrice(priceData.getMarket_price());
            entity.setLowPrice(priceData.getLow_price());
        }
    }

    default void updateImageUrl(TcgPlayerDto tcgData, PokemonCard entity) {
        String url = tcgData.getUrl();
        if (url != null && url.contains("/product/")) {
            try {
                String productId = url.substring(url.lastIndexOf("/product/") + 9);
                if (productId.contains("?")) {
                    productId = productId.substring(0, productId.indexOf("?"));
                }
                entity.setImageUrl("https://tcgplayer-cdn.tcgplayer.com/product/" + productId + "_in_1000x1000.jpg");
            } catch (Exception ignored) {
                // Keep imageUrl null if parsing fails
            }
        }
    }
}
