package com.zakpruitt.collectingwithzak.client;

import com.zakpruitt.collectingwithzak.dto.pokewallet.PokeWalletSearchResponse;
import com.zakpruitt.collectingwithzak.dto.pokewallet.PokeWalletSearchResponse.PokeWalletCard;
import com.zakpruitt.collectingwithzak.dto.pokewallet.PokeWalletSetsResponse;
import com.zakpruitt.collectingwithzak.dto.pokewallet.PokeWalletSetsResponse.PokeWalletSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PokeWalletClient {

    private final RestTemplate restTemplate;

    @Value("${pokewallet.api-key}")
    private String apiKey;
    @Value("${pokewallet.base-url}")
    private String baseUrl;

    public List<PokeWalletSet> fetchSets() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        headers.set("Accept", "application/json");

        var response = restTemplate.exchange(baseUrl + "/sets", HttpMethod.GET,
                new HttpEntity<>(headers), PokeWalletSetsResponse.class);

        var body = response.getBody();
        if (body == null || body.getData() == null) return List.of();
        return body.getData();
    }

    public List<PokeWalletCard> fetchAllCards(String query) {
        List<PokeWalletCard> all = new ArrayList<>();
        int page = 1;
        int totalPages = 1;

        while (page <= totalPages) {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/search")
                    .queryParam("q", query)
                    .queryParam("page", page)
                    .queryParam("limit", 100)
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", apiKey);
            headers.set("Accept", "application/json");

            var response = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), PokeWalletSearchResponse.class);

            var body = response.getBody();
            if (body == null || body.getResults() == null || body.getResults().isEmpty()) break;

            all.addAll(body.getResults());

            if (body.getPagination() != null) {
                totalPages = body.getPagination().getTotalPages();
            }

            page++;
        }

        return all;
    }
}
