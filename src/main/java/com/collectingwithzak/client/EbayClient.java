package com.collectingwithzak.client;

import com.collectingwithzak.dto.ebay.*;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EbayClient {

    private static final int WINDOW_DAYS = 90;
    private static final int ORDERS_PAGE_SIZE = 50;
    private static final int TRANSACTIONS_PAGE_SIZE = 200;
    private static final DateTimeFormatter EBAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private final RestTemplate restTemplate;

    @Value("${ebay.client-id}")
    private String clientId;
    @Value("${ebay.client-secret}")
    private String clientSecret;
    @Value("${ebay.refresh-token}")
    private String refreshToken;
    @Value("${ebay.url.orders}")
    private String ordersUrl;
    @Value("${ebay.url.transactions}")
    private String transactionsUrl;
    @Value("${ebay.url.token}")
    private String tokenUrl;

    private String accessToken;
    private ZonedDateTime tokenExpiry;

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank() && !clientSecret.isBlank() && !refreshToken.isBlank();
    }

    public List<EbayOrderData> fetchOrderData(ZonedDateTime since) {
        List<EbayOrder> orders = fetchOrders(since);
        List<EbayTransaction> transactions = fetchTransactions(since);

        Map<String, Double> transactionData = aggregateTransactions(transactions);
        List<EbayOrderData> results = new ArrayList<>();

        for (EbayOrder order : orders) {
            try {
                results.add(toOrderData(order, transactionData));
            } catch (Exception e) {
                log.warn("Skipping order {}: {}", order.getOrderId(), e.getMessage());
            }
        }

        log.info("eBay fetch: {} orders, {} parsed", orders.size(), results.size());
        return results;
    }

    private Map<String, Double> aggregateTransactions(List<EbayTransaction> transactions) {
        Map<String, Double> data = new HashMap<>();

        for (EbayTransaction txn : transactions) {
            String orderId = txn.getOrderId();
            if (orderId == null || orderId.isBlank()) continue;

            switch (txn.getTransactionType()) {
                case "SALE" -> {
                    data.merge(orderId + ":payout", amountOrZero(txn.getAmount()), Double::sum);
                    data.merge(orderId + ":fees", amountOrZero(txn.getTotalFeeAmount()), Double::sum);
                }
                case "SHIPPING_LABEL" -> {
                    data.merge(orderId + ":shipping", Math.abs(amountOrZero(txn.getAmount())), Double::sum);
                }
                case "NON_SALE_CHARGE", "ADJUSTMENT" -> {
                    data.merge(orderId + ":fees", Math.abs(amountOrZero(txn.getAmount())), Double::sum);
                }
                case "REFUND" -> {
                    data.merge(orderId + ":refund", Math.abs(amountOrZero(txn.getAmount())), Double::sum);
                }
                default -> log.debug("Unhandled eBay transaction type '{}' for order {} amount={}",
                        txn.getTransactionType(), orderId, amountOrZero(txn.getAmount()));
            }
        }

        return data;
    }

    // grossAmount = payout + fees (actual money collected, not listed prices)
    // netAmount is computed downstream: grossAmount - ebayFees - shippingCost

    private EbayOrderData toOrderData(EbayOrder order, Map<String, Double> transactionData) {
        String orderId = order.getOrderId();

        double payout = transactionData.getOrDefault(orderId + ":payout", 0.0);
        double fees = transactionData.getOrDefault(orderId + ":fees", 0.0);
        double refund = transactionData.getOrDefault(orderId + ":refund", 0.0);

        return EbayOrderData.builder()
                .ebayOrderId(orderId)
                .saleDate(ZonedDateTime.parse(order.getCreationDate()).toLocalDate())
                .title(firstLineItemTitle(order.getLineItems()))
                .buyerUsername(order.getBuyer() != null ? order.getBuyer().getUsername() : "")
                .grossAmount(payout + fees - refund)
                .ebayFees(fees)
                .shippingCost(transactionData.getOrDefault(orderId + ":shipping", 0.0))
                .orderStatus(order.getOrderFulfillmentStatus())
                .build();
    }

    private String firstLineItemTitle(List<EbayLineItem> lineItems) {
        if (lineItems == null || lineItems.isEmpty()) return "";
        return lineItems.getFirst().getTitle();
    }

    private double amountOrZero(EbayAmount amount) {
        return amount != null ? amount.toDouble() : 0;
    }

    private List<EbayOrder> fetchOrders(ZonedDateTime since) {
        List<EbayOrder> all = new ArrayList<>();

        for (String filter : buildTimeWindowFilters(since, "creationdate")) {
            for (int offset = 0; ; offset += ORDERS_PAGE_SIZE) {
                String url = buildPageUrl(
                        ordersUrl,
                        ORDERS_PAGE_SIZE,
                        offset,
                        filter
                );

                ResponseEntity<EbayOrdersResponse> response = authenticatedGet(url, EbayOrdersResponse.class);
                if (response.getStatusCode() == HttpStatus.NO_CONTENT || response.getBody() == null) break;

                List<EbayOrder> page = response.getBody().getOrders();
                if (page == null || page.isEmpty()) break;

                all.addAll(page);
                if (page.size() < ORDERS_PAGE_SIZE) break;
            }
        }
        return all;
    }

    private List<EbayTransaction> fetchTransactions(ZonedDateTime since) {
        List<EbayTransaction> all = new ArrayList<>();

        for (String filter : buildTimeWindowFilters(since, "transactionDate")) {
            for (int offset = 0; ; offset += TRANSACTIONS_PAGE_SIZE) {
                String url = buildPageUrl(
                        transactionsUrl,
                        TRANSACTIONS_PAGE_SIZE,
                        offset,
                        filter
                );

                ResponseEntity<EbayTransactionsResponse> response = authenticatedGet(url, EbayTransactionsResponse.class);
                if (response.getStatusCode() == HttpStatus.NO_CONTENT || response.getBody() == null) break;

                List<EbayTransaction> page = response.getBody().getTransactions();
                if (page == null || page.isEmpty()) break;

                all.addAll(page);
                if (page.size() < TRANSACTIONS_PAGE_SIZE) break;
            }
        }
        return all;
    }

    private List<String> buildTimeWindowFilters(ZonedDateTime since, String filterField) {
        List<String> filters = new ArrayList<>();
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        for (ZonedDateTime windowStart = since; windowStart.isBefore(now); windowStart = windowStart.plusDays(WINDOW_DAYS)) {
            ZonedDateTime windowEnd = windowStart.plusDays(WINDOW_DAYS);
            if (windowEnd.isAfter(now)) windowEnd = now;
            filters.add(filterField + ":[" + windowStart.format(EBAY_FMT) + ".." + windowEnd.format(EBAY_FMT) + "]");
        }
        return filters;
    }

    private String buildPageUrl(
            String baseUrl,
            int limit,
            int offset,
            String filter
    ) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("limit", limit)
                .queryParam("offset", offset)
                .queryParam("filter", filter)
                .toUriString();
    }

    private <T> ResponseEntity<T> authenticatedGet(String url, Class<T> responseType) {
        refreshTokenIfNeeded();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), responseType);
    }

    private synchronized void refreshTokenIfNeeded() {
        if (accessToken != null && tokenExpiry != null && ZonedDateTime.now(ZoneOffset.UTC).isBefore(tokenExpiry))
            return;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);

        EbayTokenResponse token = restTemplate.postForObject(
                tokenUrl, new HttpEntity<>(body, headers), EbayTokenResponse.class);
        if (token == null) throw new RuntimeException("eBay token response was null");

        accessToken = token.getAccessToken();
        tokenExpiry = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(token.getExpiresIn() - 60);
    }
}
