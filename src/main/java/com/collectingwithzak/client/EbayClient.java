package com.collectingwithzak.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EbayClient {

    private static final int WINDOW_DAYS = 90;
    private static final DateTimeFormatter EBAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private final RestTemplate restTemplate;

    @Value("${ebay.client-id}") private String clientId;
    @Value("${ebay.client-secret}") private String clientSecret;
    @Value("${ebay.refresh-token}") private String refreshToken;

    private String accessToken;
    private ZonedDateTime tokenExpiry;

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank() && !clientSecret.isBlank() && !refreshToken.isBlank();
    }

    public List<Map<String, Object>> fetchOrders(ZonedDateTime since) {
        return fetchWindowed(since, "https://api.ebay.com/sell/fulfillment/v1/order",
                "creationdate", "orders", 50);
    }

    public List<Map<String, Object>> fetchTransactions(ZonedDateTime since) {
        return fetchWindowed(since, "https://apiz.ebay.com/sell/finances/v1/transaction",
                "transactionDate", "transactions", 200);
    }

    public double parseAmount(Map<String, String> amount) {
        if (amount == null || amount.get("value") == null) return 0;
        try { return Double.parseDouble(amount.get("value")); }
        catch (NumberFormatException e) { return 0; }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchWindowed(ZonedDateTime since, String baseUrl,
                                                     String filterField, String listKey, int limit) {
        List<Map<String, Object>> all = new ArrayList<>();
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        for (var start = since; start.isBefore(now); start = start.plusDays(WINDOW_DAYS)) {
            var end = start.plusDays(WINDOW_DAYS);
            if (end.isAfter(now)) end = now;

            String filter = filterField + ":[" + start.format(EBAY_FMT) + ".." + end.format(EBAY_FMT) + "]";

            for (int offset = 0; ; offset += limit) {
                String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                        .queryParam("limit", limit).queryParam("offset", offset)
                        .queryParam("filter", filter).toUriString();

                var response = authenticatedGet(url);
                if (response.getStatusCode() == HttpStatus.NO_CONTENT || response.getBody() == null) break;

                var page = (List<Map<String, Object>>) response.getBody().get(listKey);
                if (page == null || page.isEmpty()) break;

                all.addAll(page);
                if (page.size() < limit) break;
            }
        }
        return all;
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map> authenticatedGet(String url) {
        refreshTokenIfNeeded();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    }

    @SuppressWarnings("unchecked")
    private synchronized void refreshTokenIfNeeded() {
        if (accessToken != null && tokenExpiry != null && ZonedDateTime.now(ZoneOffset.UTC).isBefore(tokenExpiry)) return;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);

        var response = restTemplate.postForObject(
                "https://api.ebay.com/identity/v1/oauth2/token", new HttpEntity<>(body, headers), Map.class);
        if (response == null) throw new RuntimeException("Token response was null");

        accessToken = (String) response.get("access_token");
        tokenExpiry = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds((int) response.get("expires_in") - 60);
    }
}
