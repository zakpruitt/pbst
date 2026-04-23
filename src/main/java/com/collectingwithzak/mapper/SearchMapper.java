package com.collectingwithzak.mapper;

import com.collectingwithzak.dto.response.CardSearchResult;
import com.collectingwithzak.dto.response.SealedSearchResult;
import com.collectingwithzak.entity.PokemonCard;
import com.collectingwithzak.entity.SealedProduct;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SearchMapper {

    CardSearchResult cardToSearchResult(PokemonCard entity);

    List<CardSearchResult> cardsToSearchResults(List<PokemonCard> entities);

    SealedSearchResult sealedToSearchResult(SealedProduct entity);

    List<SealedSearchResult> sealedToSearchResults(List<SealedProduct> entities);
}
