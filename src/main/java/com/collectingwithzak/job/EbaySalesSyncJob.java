package com.collectingwithzak.job;

import com.collectingwithzak.client.EbayClient;
import com.collectingwithzak.service.SaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class EbaySalesSyncJob {

    private static final int SYNC_DAYS = 720;

    private final EbayClient ebayClient;
    private final SaleService saleService;

    @Scheduled(fixedRate = 3_600_000)
    public void sync() {
        if (!ebayClient.isConfigured()) {
            log.info("eBay sync skipped: credentials not configured");
            return;
        }
        try {
            ZonedDateTime since = ZonedDateTime.now(ZoneOffset.UTC).minusDays(SYNC_DAYS);
            saleService.syncFromEbay(ebayClient.fetchOrders(since), ebayClient.fetchTransactions(since));
        } catch (Exception e) {
            log.error("eBay sync failed", e);
        }
    }
}
