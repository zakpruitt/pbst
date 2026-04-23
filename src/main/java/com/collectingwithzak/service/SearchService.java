package com.collectingwithzak.service;

import com.collectingwithzak.dto.response.CardSearchResult;
import com.collectingwithzak.dto.response.SealedSearchResult;
import com.collectingwithzak.mapper.SearchMapper;
import com.collectingwithzak.repository.PokemonCardRepository;
import com.collectingwithzak.repository.SealedProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final PokemonCardRepository cardRepo;
    private final SealedProductRepository sealedRepo;
    private final SearchMapper searchMapper;

    public List<CardSearchResult> searchCards(String query) {
        if (query == null || query.isBlank()) return List.of();
        return searchMapper.cardsToSearchResults(cardRepo.search(query));
    }

    public List<SealedSearchResult> searchSealed(String query) {
        if (query == null || query.isBlank()) return List.of();
        return searchMapper.sealedToSearchResults(sealedRepo.search(query));
    }
}
