package com.collectingwithzak.service;

import com.collectingwithzak.client.EbayClient;
import com.collectingwithzak.dto.ebay.EbayOrderData;
import com.collectingwithzak.dto.request.CreateSaleRequest;
import com.collectingwithzak.entity.Sale;
import com.collectingwithzak.exception.ResourceNotFoundException;
import com.collectingwithzak.mapper.SaleMapper;
import com.collectingwithzak.repository.SaleRepository;
import com.collectingwithzak.repository.TrackedItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SaleService {

    private final SaleRepository saleRepo;
    private final TrackedItemRepository itemRepo;
    private final SaleMapper saleMapper;
    private final EbayClient ebayClient;

    public Sale create(CreateSaleRequest request) {
        Sale sale = saleMapper.toEntity(request);
        if (sale.getOrigin() == null || sale.getOrigin().isBlank()) {
            sale.setOrigin("EBAY");
        }
        return saleRepo.save(sale);
    }

    @Transactional(readOnly = true)
    public Sale getById(Long id) {
        return saleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", id));
    }

    @Transactional(readOnly = true)
    public List<Sale> getAll(String view) {
        return switch (view) {
            case "vince" -> saleRepo.findVince();
            case "ignored" -> saleRepo.findIgnored();
            default -> saleRepo.findConfirmed();
        };
    }

    @Transactional(readOnly = true)
    public List<Sale> getStaged() {
        return saleRepo.findByStatusOrderBySaleDateDesc("STAGED");
    }

    @Transactional(readOnly = true)
    public long countStaged() {
        return saleRepo.countByStatus("STAGED");
    }

    public void confirmWithItems(Long saleId, List<Long> itemIds) {
        itemRepo.detachFromSale(saleId);
        if (itemIds != null && !itemIds.isEmpty()) {
            itemRepo.attachToSale(itemIds, saleId);
        }
        saleRepo.updateStatus(saleId, "CONFIRMED");
    }

    public void ignore(Long saleId) {
        itemRepo.detachFromSale(saleId);
        saleRepo.updateStatusAndAttribution(saleId, "IGNORED", "");
    }

    // Stored as IGNORED so every KPI query (which filters on CONFIRMED) already
    // excludes it, plus the attributed_to flag lets us surface Vince's sales on
    // a dedicated tab.
    public void markAsVince(Long saleId) {
        itemRepo.detachFromSale(saleId);
        saleRepo.updateStatusAndAttribution(saleId, "IGNORED", "vince");
    }

    public void unstage(Long saleId) {
        itemRepo.detachFromSale(saleId);
        saleRepo.updateStatusAndAttribution(saleId, "STAGED", "");
    }

    public void delete(Long saleId) {
        itemRepo.detachFromSale(saleId);
        saleRepo.deleteById(saleId);
    }

    public void syncFromEbay(List<Map<String, Object>> orders, List<Map<String, Object>> transactions) {
        Map<String, Double> fees = collectFees(transactions);
        int upserted = 0;
        for (var order : orders) {
            try {
                upsertFromEbay(saleMapper.fromEbayOrder(extractOrderData(order, fees)));
                upserted++;
            } catch (Exception e) {
                log.warn("Skipping order {}: {}", order.get("orderId"), e.getMessage());
            }
        }
        log.info("eBay sync: {} orders, {} upserted", orders.size(), upserted);
    }

    private void upsertFromEbay(Sale sale) {
        Sale existing = saleRepo.findByEbayOrderId(sale.getEbayOrderId());
        if (existing != null) {
            existing.setGrossAmount(sale.getGrossAmount());
            existing.setEbayFees(sale.getEbayFees());
            existing.setShippingCost(sale.getShippingCost());
            existing.setNetAmount(sale.getNetAmount());
            existing.setOrderStatus(sale.getOrderStatus());
            existing.setTitle(sale.getTitle());
            existing.setBuyerUsername(sale.getBuyerUsername());
            saleRepo.save(existing);
        } else {
            sale.setStatus("STAGED");
            saleRepo.save(sale);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> collectFees(List<Map<String, Object>> transactions) {
        Map<String, Double> fees = new HashMap<>();
        for (var txn : transactions) {
            String orderId = (String) txn.get("orderId");
            if (orderId == null || orderId.isBlank()) continue;

            double amount = switch ((String) txn.get("transactionType")) {
                case "SALE" -> ebayClient.parseAmount((Map<String, String>) txn.get("totalFeeAmount"));
                case "SHIPPING_LABEL", "NON_SALE_CHARGE" -> Math.abs(ebayClient.parseAmount((Map<String, String>) txn.get("amount")));
                default -> 0;
            };
            if (amount != 0) fees.merge(orderId, amount, Double::sum);
        }
        return fees;
    }

    @SuppressWarnings("unchecked")
    private EbayOrderData extractOrderData(Map<String, Object> order, Map<String, Double> fees) {
        String orderId = (String) order.get("orderId");
        var pricing = (Map<String, Object>) order.get("pricingSummary");
        var lineItems = (List<Map<String, Object>>) order.get("lineItems");
        var buyer = (Map<String, Object>) order.get("buyer");

        EbayOrderData data = new EbayOrderData();
        data.setEbayOrderId(orderId);
        data.setSaleDate(ZonedDateTime.parse((String) order.get("creationDate")).toLocalDate());
        data.setTitle(lineItems != null && !lineItems.isEmpty() ? (String) lineItems.getFirst().get("title") : "");
        data.setBuyerUsername(buyer != null ? (String) buyer.get("username") : "");
        data.setGrossAmount(ebayClient.parseAmount((Map<String, String>) pricing.get("total")));
        data.setEbayFees(fees.getOrDefault(orderId, 0.0));
        data.setShippingCost(ebayClient.parseAmount((Map<String, String>) pricing.get("deliveryCost")));
        data.setOrderStatus((String) order.get("orderFulfillmentStatus"));
        return data;
    }
}
