package com.collectingwithzak.client;

import com.collectingwithzak.dto.pokewallet.PokeWalletSearchResponse;
import com.collectingwithzak.dto.pokewallet.PokeWalletSearchResponse.PokeWalletCard;
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

    @Value("${pokewallet.api-key}") private String apiKey;
    @Value("${pokewallet.base-url}") private String baseUrl;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && baseUrl != null && !baseUrl.isBlank();
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
            if (page <= totalPages) sleep(500);
        }

        return all;
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
