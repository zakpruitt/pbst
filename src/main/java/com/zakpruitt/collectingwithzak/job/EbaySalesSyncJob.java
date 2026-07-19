package com.zakpruitt.collectingwithzak.job;

import com.zakpruitt.collectingwithzak.config.JbayProvider;
import com.zakpruitt.collectingwithzak.service.EbayOrderDataService;
import com.zakpruitt.collectingwithzak.service.SaleService;
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

    private final JbayProvider jbayProvider;
    private final EbayOrderDataService ebayOrderDataService;
    private final SaleService saleService;

    @Scheduled(fixedRate = 3_600_000)
    public void sync() {
        if (!jbayProvider.isConfigured()) {
            log.info("eBay sync skipped: credentials not configured");
            return;
        }
        try {
            ZonedDateTime since = ZonedDateTime.now(ZoneOffset.UTC).minusDays(SYNC_DAYS);
            saleService.syncFromEbay(ebayOrderDataService.fetchOrderData(since));
        } catch (Exception e) {
            log.error("eBay sync failed", e);
        }
    }
}
