package com.zakpruitt.pbst.clients;

import com.zakpruitt.pbst.dtos.pokewallet.PokeWalletCardDto;
import com.zakpruitt.pbst.dtos.pokewallet.PokeWalletSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PokeWalletClient {

    private final RestClient restClient;

    public PokeWalletClient(@Value("${pokewallet.api.url}") String baseUrl,
                            @Value("${pokewallet.api.key}") String apiKey) {

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-API-Key", apiKey)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public List<PokeWalletCardDto> fetchAllCardsByQuery(String query) {
        List<PokeWalletCardDto> allCards = new ArrayList<>();
        int currentPage = 1;
        int totalPages = 1;

        log.info("Starting card sync for query: {}", query);
        while (currentPage <= totalPages) {
            PokeWalletSearchResponse response = fetchSearchPage(query, currentPage, 100);
            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                log.warn("Received empty or null response on page {} for query {}", currentPage, query);
                break;
            }

            allCards.addAll(response.getResults());

            if (response.getPagination() != null) {
                totalPages = response.getPagination().getTotal_pages();
            }
            currentPage++;
            if (currentPage <= totalPages) {
                sleepForRateLimit();
            }
        }

        log.info("Finished syncing. Fetched {} total cards for query: {}", allCards.size(), query);
        return allCards;
    }

    private PokeWalletSearchResponse fetchSearchPage(String query, int page, int limit) {
        log.debug("Fetching page {} (limit {}) for query: {}", page, limit, query);

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", query)
                        .queryParam("page", page)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .body(PokeWalletSearchResponse.class);
    }

    private void sleepForRateLimit() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Rate limit sleep was interrupted", e);
        }
    }
}