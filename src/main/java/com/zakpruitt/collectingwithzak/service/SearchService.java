package com.zakpruitt.collectingwithzak.service;

import com.zakpruitt.collectingwithzak.entity.PokemonCard;
import com.zakpruitt.collectingwithzak.entity.SealedProduct;
import com.zakpruitt.collectingwithzak.repository.PokemonCardRepository;
import com.zakpruitt.collectingwithzak.repository.SealedProductRepository;
import com.zakpruitt.collectingwithzak.repository.SearchSpecification;
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

    public List<PokemonCard> searchCards(String query) {
        if (!StringUtils.hasText(query)) return List.of();
        return cardRepo.findAll(SearchSpecification.multiTermLike(query, CARD_SEARCH_FIELDS),
                PageRequest.of(0, MAX_RESULTS)).getContent();
    }

    public List<SealedProduct> searchSealed(String query) {
        if (!StringUtils.hasText(query)) return List.of();
        return sealedRepo.findAll(SearchSpecification.multiTermLike(query, SEALED_SEARCH_FIELDS),
                PageRequest.of(0, MAX_RESULTS)).getContent();
    }
}
