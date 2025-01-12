package com.zakpruitt.pbst.services;

import com.zakpruitt.pbst.dtos.PokemonCardSearchDTO;
import com.zakpruitt.pbst.entities.PokemonCard;
import com.zakpruitt.pbst.exception.ResourceNotFoundException;
import com.zakpruitt.pbst.mappers.PokemonCardSearchMapper;
import com.zakpruitt.pbst.repositories.PokemonCardRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@AllArgsConstructor
public class PokemonCardService {

    private final PokemonCardRepository pokemonCardRepository;
    private final PokemonCardSearchMapper pokemonCardSearchMapper;

    public List<PokemonCardSearchDTO> searchCards(String query) {
        // For now, a simple search by name. Can be expanded later.
        List<PokemonCard> cards = pokemonCardRepository.findByNameContainingIgnoreCase(query);
        return cards.stream()
                .map(pokemonCardSearchMapper::toDto)
                .collect(Collectors.toList());
    }

    public PokemonCardSearchDTO getPokemonCardSearchDTOById(String id) {
        return pokemonCardRepository.findById(id)
                .map(pokemonCardSearchMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("PokemonCard not found with id: " + id));
    }
}
