package com.collectingwithzak.mapper;

import com.collectingwithzak.dto.inventory.PokemonCardResponse;
import com.collectingwithzak.dto.inventory.SealedProductResponse;
import com.collectingwithzak.entity.PokemonCard;
import com.collectingwithzak.entity.SealedProduct;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SearchMapper {

    PokemonCardResponse cardToSearchResult(PokemonCard entity);

    List<PokemonCardResponse> cardsToSearchResults(List<PokemonCard> entities);

    SealedProductResponse sealedToSearchResult(SealedProduct entity);

    List<SealedProductResponse> sealedToSearchResults(List<SealedProduct> entities);
}
