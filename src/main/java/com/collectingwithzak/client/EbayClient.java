package com.collectingwithzak.client;

import com.collectingwithzak.dto.ebay.EbayOrderData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EbayClient {

    private static final int WINDOW_DAYS = 90;
    private static final DateTimeFormatter EBAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;

    @Value("${ebay.client-id}") private String clientId;
    @Value("${ebay.client-secret}") private String clientSecret;
    @Value("${ebay.refresh-token}") private String refreshToken;

    private String accessToken;
    private ZonedDateTime tokenExpiry;

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank() && !clientSecret.isBlank() && !refreshToken.isBlank();
    }

    public List<EbayOrderData> fetchOrderData(ZonedDateTime since) {
        List<Map<String, Object>> orders = fetchWindowed(since,
                "https://api.ebay.com/sell/fulfillment/v1/order", "creationdate", "orders", 50);
        List<Map<String, Object>> transactions = fetchWindowed(since,
                "https://apiz.ebay.com/sell/finances/v1/transaction", "transactionDate", "transactions", 200);

        Map<String, Double> fees = collectFees(transactions);
        List<EbayOrderData> results = new ArrayList<>();

        for (Map<String, Object> order : orders) {
            try {
                results.add(extractOrderData(order, fees));
            } catch (Exception e) {
                log.warn("Skipping order {}: {}", order.get("orderId"), e.getMessage());
            }
        }

        log.info("eBay fetch: {} orders, {} parsed", orders.size(), results.size());
        return results;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> collectFees(List<Map<String, Object>> transactions) {
        Map<String, Double> fees = new HashMap<>();

        for (Map<String, Object> txn : transactions) {
            String orderId = (String) txn.get("orderId");
            if (orderId == null || orderId.isBlank()) continue;

            String type = (String) txn.get("transactionType");
            if ("SALE".equals(type)) {
                double fee = parseAmount((Map<String, String>) txn.get("totalFeeAmount"));
                if (fee != 0) fees.merge(orderId + ":fees", fee, Double::sum);
            } else if ("SHIPPING_LABEL".equals(type) || "NON_SALE_CHARGE".equals(type)) {
                double label = Math.abs(parseAmount((Map<String, String>) txn.get("amount")));
                if (label != 0) fees.merge(orderId + ":shipping", label, Double::sum);
            } else if ("REFUND".equals(type)) {
                double refund = Math.abs(parseAmount((Map<String, String>) txn.get("amount")));
                if (refund != 0) fees.merge(orderId + ":refund", refund, Double::sum);
            }
        }

        return fees;
    }

    @SuppressWarnings("unchecked")
    private EbayOrderData extractOrderData(Map<String, Object> order, Map<String, Double> fees) {
        String orderId = (String) order.get("orderId");
        var pricing = (Map<String, Object>) order.get("pricingSummary");
        var lineItems = (List<Map<String, Object>>) order.get("lineItems");
        var buyer = (Map<String, Object>) order.get("buyer");

        double subtotal = parseAmount((Map<String, String>) pricing.get("priceSubtotal"));
        double buyerShipping = parseAmount((Map<String, String>) pricing.get("deliveryCost"));
        double refund = fees.getOrDefault(orderId + ":refund", 0.0);

        EbayOrderData data = new EbayOrderData();
        data.setEbayOrderId(orderId);
        data.setSaleDate(ZonedDateTime.parse((String) order.get("creationDate")).toLocalDate());
        data.setTitle(lineItems != null && !lineItems.isEmpty() ? (String) lineItems.getFirst().get("title") : "");
        data.setBuyerUsername(buyer != null ? (String) buyer.get("username") : "");
        data.setGrossAmount(subtotal + buyerShipping - refund);
        data.setEbayFees(fees.getOrDefault(orderId + ":fees", 0.0));
        data.setShippingCost(fees.getOrDefault(orderId + ":shipping", 0.0));
        data.setOrderStatus((String) order.get("orderFulfillmentStatus"));
        return data;
    }

    private double parseAmount(Map<String, String> amount) {
        if (amount == null || amount.get("value") == null) return 0;
        try {
            return Double.parseDouble(amount.get("value"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchWindowed(ZonedDateTime since, String baseUrl,
                                                     String filterField, String listKey, int limit) {
        List<Map<String, Object>> all = new ArrayList<>();
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        for (ZonedDateTime start = since; start.isBefore(now); start = start.plusDays(WINDOW_DAYS)) {
            ZonedDateTime end = start.plusDays(WINDOW_DAYS);
            if (end.isAfter(now)) end = now;

            String filter = filterField + ":[" + start.format(EBAY_FMT) + ".." + end.format(EBAY_FMT) + "]";

            for (int offset = 0; ; offset += limit) {
                String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                        .queryParam("limit", limit).queryParam("offset", offset)
                        .queryParam("filter", filter).toUriString();

                ResponseEntity<Map<String, Object>> response = authenticatedGet(url);
                if (response.getStatusCode() == HttpStatus.NO_CONTENT || response.getBody() == null) break;

                var page = (List<Map<String, Object>>) response.getBody().get(listKey);
                if (page == null || page.isEmpty()) break;

                all.addAll(page);
                if (page.size() < limit) break;
            }
        }
        return all;
    }

    private ResponseEntity<Map<String, Object>> authenticatedGet(String url) {
        refreshTokenIfNeeded();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), MAP_TYPE);
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
        if (response == null) throw new RuntimeException("eBay token response was null");

        accessToken = (String) response.get("access_token");
        tokenExpiry = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds((int) response.get("expires_in") - 60);
    }
}
