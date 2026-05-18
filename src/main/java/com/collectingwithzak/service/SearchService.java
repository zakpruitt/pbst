package com.collectingwithzak.service;

import com.collectingwithzak.dto.response.CardSearchResult;
import com.collectingwithzak.dto.response.SealedSearchResult;
import com.collectingwithzak.mapper.SearchMapper;
import com.collectingwithzak.repository.PokemonCardRepository;
import com.collectingwithzak.repository.SearchSpecification;
import com.collectingwithzak.repository.SealedProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private static final List<String> CARD_SEARCH_FIELDS = List.of(
            "name",
            "setName",
            "setCode",
            "cardNumber",
            "id"
    );
    private static final List<String> SEALED_SEARCH_FIELDS = List.of("name", "setName");
    private static final int MAX_RESULTS = 15;

    private final PokemonCardRepository cardRepo;
    private final SealedProductRepository sealedRepo;
    private final SearchMapper searchMapper;

    public List<CardSearchResult> searchCards(String query) {
        if (!StringUtils.hasText(query)) return List.of();
        return searchMapper.cardsToSearchResults(
                cardRepo.findAll(SearchSpecification.multiTermLike(query, CARD_SEARCH_FIELDS), PageRequest.of(0, MAX_RESULTS)).getContent());
    }

    public List<SealedSearchResult> searchSealed(String query) {
        if (!StringUtils.hasText(query)) return List.of();
        return searchMapper.sealedToSearchResults(
                sealedRepo.findAll(SearchSpecification.multiTermLike(query, SEALED_SEARCH_FIELDS), PageRequest.of(0, MAX_RESULTS)).getContent());
    }
}
