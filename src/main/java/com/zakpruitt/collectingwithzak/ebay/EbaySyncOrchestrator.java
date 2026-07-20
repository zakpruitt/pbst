package com.zakpruitt.collectingwithzak.ebay;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EbaySyncOrchestrator {

    private final EbaySaleUpsertService ebaySaleUpsertService;

    public void sync(List<EbayOrderData> orders) {
        int upserted = 0;
        for (EbayOrderData order : orders) {
            try {
                ebaySaleUpsertService.upsertFromEbay(order);
                upserted++;
            } catch (Exception e) {
                log.warn("Skipping order {}: {}", order.getEbayOrderId(), e.getMessage());
            }
        }
        log.info("eBay sync: {} orders, {} upserted", orders.size(), upserted);
    }
}
