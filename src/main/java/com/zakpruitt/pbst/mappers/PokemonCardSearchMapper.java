package com.zakpruitt.pbst.mappers;

import com.zakpruitt.pbst.dtos.PokemonCardSearchDTO;
import com.zakpruitt.pbst.entities.PokemonCard;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface PokemonCardSearchMapper {
    PokemonCardSearchDTO toDto(PokemonCard pokemonCard);
}
