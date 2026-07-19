package com.zakpruitt.collectingwithzak.service;

import com.zakpruitt.collectingwithzak.config.JbayProvider;
import com.zakpruitt.collectingwithzak.dto.ebay.EbayOrderData;
import com.zakpruitt.jbay.Amount;
import com.zakpruitt.jbay.finances.Transaction;
import com.zakpruitt.jbay.orders.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class EbayOrderDataService {

    private final JbayProvider jbayProvider;

    public List<EbayOrderData> fetchOrderData(ZonedDateTime since) {
        List<Order> orders = jbayProvider.client().orders().since(since);
        List<Transaction> transactions = jbayProvider.client().finances().transactionsSince(since);

        Map<String, Double> transactionData = aggregateTransactions(transactions);
        List<EbayOrderData> results = new ArrayList<>();

        for (Order order : orders) {
            try {
                results.add(toOrderData(order, transactionData));
            } catch (Exception e) {
                log.warn("Skipping order {}: {}", order.orderId(), e.getMessage());
            }
        }

        log.info("eBay fetch: {} orders, {} parsed", orders.size(), results.size());
        return results;
    }

    private Map<String, Double> aggregateTransactions(List<Transaction> transactions) {
        Map<String, Double> data = new HashMap<>();

        for (Transaction txn : transactions) {
            String orderId = txn.orderId();
            if (!StringUtils.hasText(orderId)) continue;

            switch (txn.transactionType()) {
                case "SALE" -> {
                    data.merge(orderId + ":payout", Amount.orZero(txn.amount()), Double::sum);
                    data.merge(orderId + ":fees", Amount.orZero(txn.totalFeeAmount()), Double::sum);
                }
                case "SHIPPING_LABEL" -> {
                    data.merge(orderId + ":shipping", Math.abs(Amount.orZero(txn.amount())), Double::sum);
                }
                case "NON_SALE_CHARGE", "ADJUSTMENT" -> {
                    data.merge(orderId + ":fees", Math.abs(Amount.orZero(txn.amount())), Double::sum);
                }
                case "REFUND" -> {
                    data.merge(orderId + ":refund", Math.abs(Amount.orZero(txn.amount())), Double::sum);
                }
                default -> log.debug("Unhandled eBay transaction type '{}' for order {} amount={}",
                        txn.transactionType(), orderId, Amount.orZero(txn.amount()));
            }
        }

        return data;
    }

    // grossAmount = payout + fees (actual money collected, not listed prices)
    // netAmount is computed downstream: grossAmount - ebayFees - shippingCost

    private EbayOrderData toOrderData(Order order, Map<String, Double> transactionData) {
        String orderId = order.orderId();

        double payout = transactionData.getOrDefault(orderId + ":payout", 0.0);
        double fees = transactionData.getOrDefault(orderId + ":fees", 0.0);
        double refund = transactionData.getOrDefault(orderId + ":refund", 0.0);

        return EbayOrderData.builder()
                .ebayOrderId(orderId)
                .saleDate(order.creationDateTime().toLocalDate())
                .title(order.lineItems().isEmpty() ? "" : order.lineItems().getFirst().title())
                .buyerUsername(order.buyerUsername())
                .grossAmount(payout + fees - refund)
                .ebayFees(fees)
                .shippingCost(transactionData.getOrDefault(orderId + ":shipping", 0.0))
                .orderStatus(order.orderFulfillmentStatus())
                .build();
    }
}
