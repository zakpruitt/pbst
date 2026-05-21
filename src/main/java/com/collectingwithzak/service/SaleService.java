package com.collectingwithzak.service;

import com.collectingwithzak.dto.ebay.EbayOrderData;
import com.collectingwithzak.dto.request.CreateSaleRequest;
import com.collectingwithzak.dto.request.CreateVincePaymentRequest;
import com.collectingwithzak.entity.enums.SaleAction;
import com.collectingwithzak.entity.enums.SaleStatus;
import com.collectingwithzak.mapper.SaleMapper;
import com.collectingwithzak.mapper.VincePaymentMapper;
import com.collectingwithzak.repository.SaleRepository;
import com.collectingwithzak.repository.TrackedItemRepository;
import com.collectingwithzak.repository.VincePaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SaleService {

    private final SaleRepository saleRepo;
    private final TrackedItemRepository itemRepo;
    private final VincePaymentRepository paymentRepo;
    private final SaleMapper saleMapper;
    private final VincePaymentMapper paymentMapper;
    private final EbaySaleUpsertService ebaySaleUpsertService;

    public void create(CreateSaleRequest request) {
        saleRepo.save(saleMapper.toEntity(request));
    }

    public void syncFromEbay(List<EbayOrderData> orders) {
        int upserted = 0;
        for (EbayOrderData order : orders) {
            try {
                ebaySaleUpsertService.upsertFromEbay(saleMapper.fromEbayOrder(order));
                upserted++;
            } catch (Exception e) {
                log.warn("Skipping order {}: {}", order.getEbayOrderId(), e.getMessage());
            }
        }
        log.info("eBay sync: {} orders, {} upserted", orders.size(), upserted);
    }

    public void confirmWithItems(Long saleId, List<Long> itemIds) {
        itemRepo.detachFromSale(saleId);
        if (!itemIds.isEmpty()) {
            itemRepo.attachToSale(itemIds, saleId);
        }
        saleRepo.updateStatus(saleId, SaleStatus.CONFIRMED);
    }

    public void updateStatus(Long saleId, SaleAction action) {
        switch (action) {
            case IGNORE -> changeStatus(saleId, SaleStatus.IGNORED, "");
            case VINCE -> changeStatus(saleId, SaleStatus.IGNORED, "vince");
            case UNSTAGE -> changeStatus(saleId, SaleStatus.STAGED, "");
        }
    }

    public void updateAmounts(Long saleId, double grossAmount, double netAmount) {
        saleRepo.updateAmounts(saleId, grossAmount, netAmount);
    }

    public void delete(Long saleId) {
        itemRepo.detachFromSale(saleId);
        saleRepo.deleteById(saleId);
    }

    public void createVincePayment(CreateVincePaymentRequest request) {
        paymentRepo.save(paymentMapper.toEntity(request));
    }

    public void deleteVincePayment(Long id) {
        paymentRepo.deleteById(id);
    }

    private void changeStatus(Long saleId, SaleStatus status, String attributedTo) {
        itemRepo.detachFromSale(saleId);
        saleRepo.updateStatusAndAttribution(saleId, status, attributedTo);
    }
}
