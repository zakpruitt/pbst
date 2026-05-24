package com.zakpruitt.collectingwithzak.service.render;

import com.zakpruitt.collectingwithzak.dto.response.PokemonCardResponse;
import com.zakpruitt.collectingwithzak.dto.response.SealedProductResponse;
import com.zakpruitt.collectingwithzak.mapper.PokemonCardMapper;
import com.zakpruitt.collectingwithzak.mapper.SealedProductMapper;
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
public class SearchRenderService {

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
    private final PokemonCardMapper cardMapper;
    private final SealedProductMapper sealedMapper;

    public List<PokemonCardResponse> searchCards(String query) {
        if (!StringUtils.hasText(query)) return List.of();
        return cardMapper.toResponseList(
                cardRepo.findAll(SearchSpecification.multiTermLike(query, CARD_SEARCH_FIELDS), PageRequest.of(0, MAX_RESULTS)).getContent());
    }

    public List<SealedProductResponse> searchSealed(String query) {
        if (!StringUtils.hasText(query)) return List.of();
        return sealedMapper.toResponseList(
                sealedRepo.findAll(SearchSpecification.multiTermLike(query, SEALED_SEARCH_FIELDS), PageRequest.of(0, MAX_RESULTS)).getContent());
    }
}
