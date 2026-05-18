package com.collectingwithzak.service;

import com.collectingwithzak.dto.response.CardSearchResult;
import com.collectingwithzak.dto.response.SealedSearchResult;
import com.collectingwithzak.entity.PokemonCard;
import com.collectingwithzak.mapper.SearchMapper;
import com.collectingwithzak.repository.PokemonCardRepository;
import com.collectingwithzak.repository.SealedProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final PokemonCardRepository cardRepo;
    private final SealedProductRepository sealedRepo;
    private final SearchMapper searchMapper;
    private final EntityManager entityManager;

    public List<CardSearchResult> searchCards(String query) {
        if (!StringUtils.hasText(query)) return List.of();

        String[] terms = query.trim().toLowerCase().split("\\s+");
        StringBuilder jpql = new StringBuilder("SELECT c FROM PokemonCard c WHERE ");

        for (int i = 0; i < terms.length; i++) {
            if (i > 0) jpql.append(" AND ");
            String param = "t" + i;
            jpql.append("(LOWER(c.name) LIKE :").append(param)
                .append(" OR LOWER(c.setName) LIKE :").append(param)
                .append(" OR LOWER(c.setCode) LIKE :").append(param)
                .append(" OR LOWER(c.cardNumber) LIKE :").append(param)
                .append(" OR LOWER(c.id) LIKE :").append(param)
                .append(")");
        }
        jpql.append(" ORDER BY c.name");

        TypedQuery<PokemonCard> typedQuery = entityManager.createQuery(jpql.toString(), PokemonCard.class);
        for (int i = 0; i < terms.length; i++) {
            typedQuery.setParameter("t" + i, "%" + terms[i] + "%");
        }
        typedQuery.setMaxResults(15);

        return searchMapper.cardsToSearchResults(typedQuery.getResultList());
    }

    public List<SealedSearchResult> searchSealed(String query) {
        if (!StringUtils.hasText(query)) return List.of();
        return searchMapper.sealedToSearchResults(sealedRepo.search(query));
    }
}
