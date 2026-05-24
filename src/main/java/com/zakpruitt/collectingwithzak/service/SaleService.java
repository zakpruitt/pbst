package com.zakpruitt.collectingwithzak.service;

import com.zakpruitt.collectingwithzak.dto.ebay.EbayOrderData;
import com.zakpruitt.collectingwithzak.dto.request.CreateSaleRequest;
import com.zakpruitt.collectingwithzak.dto.request.CreateVincePaymentRequest;
import com.zakpruitt.collectingwithzak.entity.Sale;
import com.zakpruitt.collectingwithzak.entity.enums.ItemStatus;
import com.zakpruitt.collectingwithzak.entity.enums.SaleAction;
import com.zakpruitt.collectingwithzak.entity.enums.SaleStatus;
import com.zakpruitt.collectingwithzak.exception.ResourceNotFoundException;
import com.zakpruitt.collectingwithzak.mapper.SaleMapper;
import com.zakpruitt.collectingwithzak.mapper.VincePaymentMapper;
import com.zakpruitt.collectingwithzak.repository.SaleRepository;
import com.zakpruitt.collectingwithzak.repository.TrackedItemRepository;
import com.zakpruitt.collectingwithzak.repository.VincePaymentRepository;
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
        Sale sale = findWithItemsById(saleId);
        sale.getItems().forEach(item -> {
            item.setSale(null);
            item.setStatus(ItemStatus.AVAILABLE);
        });
        if (!itemIds.isEmpty()) {
            itemRepo.findAllById(itemIds).forEach(item -> {
                item.setSale(sale);
                item.setStatus(ItemStatus.SOLD);
            });
        }
        sale.setStatus(SaleStatus.CONFIRMED);
    }

    public void updateStatus(Long saleId, SaleAction action) {
        switch (action) {
            case IGNORE -> changeStatus(saleId, SaleStatus.IGNORED, "");
            case VINCE -> changeStatus(saleId, SaleStatus.IGNORED, "vince");
            case UNSTAGE -> changeStatus(saleId, SaleStatus.STAGED, "");
        }
    }

    public void updateAmounts(Long saleId, double grossAmount, double netAmount) {
        Sale sale = saleRepo.findById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", saleId));
        sale.setGrossAmount(grossAmount);
        sale.setNetAmount(netAmount);
    }

    public void delete(Long saleId) {
        Sale sale = findWithItemsById(saleId);
        sale.getItems().forEach(item -> {
            item.setSale(null);
            item.setStatus(ItemStatus.AVAILABLE);
        });
        saleRepo.delete(sale);
    }

    public void createVincePayment(CreateVincePaymentRequest request) {
        paymentRepo.save(paymentMapper.toEntity(request));
    }

    public void deleteVincePayment(Long id) {
        paymentRepo.deleteById(id);
    }

    private void changeStatus(Long saleId, SaleStatus status, String attributedTo) {
        Sale sale = findWithItemsById(saleId);
        sale.getItems().forEach(item -> {
            item.setSale(null);
            item.setStatus(ItemStatus.AVAILABLE);
        });
        sale.setStatus(status);
        sale.setAttributedTo(attributedTo);
    }

    private Sale findWithItemsById(Long saleId) {
        return saleRepo.findWithItemsById(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", saleId));
    }
}
