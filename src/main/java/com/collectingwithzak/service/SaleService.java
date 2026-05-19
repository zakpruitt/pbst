package com.collectingwithzak.service;

import com.collectingwithzak.dto.common.MonthGroup;
import com.collectingwithzak.dto.ebay.EbayOrderData;
import com.collectingwithzak.dto.inventory.TrackedItemResponse;
import com.collectingwithzak.dto.sale.CreateSaleRequest;
import com.collectingwithzak.dto.sale.SaleIndexData;
import com.collectingwithzak.dto.sale.SaleResponse;
import com.collectingwithzak.dto.vince.VincePaymentResponse;
import com.collectingwithzak.entity.Sale;
import com.collectingwithzak.entity.TrackedItem;
import com.collectingwithzak.entity.enums.SaleAction;
import com.collectingwithzak.entity.enums.SaleStatus;
import com.collectingwithzak.exception.ResourceNotFoundException;
import com.collectingwithzak.mapper.SaleMapper;
import com.collectingwithzak.mapper.TrackedItemMapper;
import com.collectingwithzak.repository.SaleRepository;
import com.collectingwithzak.repository.TrackedItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SaleService {

    private final SaleRepository saleRepo;
    private final TrackedItemRepository itemRepo;
    private final SaleMapper saleMapper;
    private final TrackedItemMapper trackedItemMapper;
    private final EbaySaleUpsertService ebaySaleUpsertService;
    private final VincePaymentService vincePaymentService;

    public SaleIndexData getIndexData(String view) {
        List<SaleResponse> sales = getAll(view);
        List<MonthGroup<SaleResponse>> groups = MonthGroup.groupByMonth(sales, SaleResponse::getSaleDate, SaleResponse::getNetAmount);

        SaleIndexData.SaleIndexDataBuilder builder = SaleIndexData.builder()
                .groups(groups)
                .stagedCount(countStaged())
                .view(view);

        if ("vince".equals(view)) {
            builder.vinceLedger(vincePaymentService.getLedger());
            List<VincePaymentResponse> payments = vincePaymentService.getAll();
            List<MonthGroup<VincePaymentResponse>> paymentGroups = MonthGroup.groupByMonth(payments, VincePaymentResponse::getPaymentDate, VincePaymentResponse::getAmount);
            builder.vincePaymentGroups(paymentGroups);
        }

        return builder.build();
    }

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

    public SaleResponse getByIdWithItems(Long id) {
        Sale sale = saleRepo.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", id));
        return saleMapper.toResponse(sale);
    }

    public List<SaleResponse> getAll(String view) {
        List<Sale> sales = switch (view) {
            case "vince" -> saleRepo.findVince();
            case "ignored" -> saleRepo.findIgnored();
            default -> saleRepo.findConfirmed();
        };
        return saleMapper.toResponseList(sales);
    }

    public List<SaleResponse> getStaged() {
        return saleMapper.toResponseList(saleRepo.findByStatusOrderBySaleDateDesc(SaleStatus.STAGED.name()));
    }

    public long countStaged() {
        return saleRepo.countByStatus(SaleStatus.STAGED.name());
    }

    public List<TrackedItemResponse> getAvailableItemsForSale(Long saleId) {
        Sale sale = saleRepo.findByIdWithItems(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", saleId));

        List<TrackedItem> available = itemRepo.findAvailableInventory();
        List<TrackedItem> allItems = new ArrayList<>(available);
        for (TrackedItem attached : sale.getItems()) {
            if (!allItems.contains(attached)) {
                allItems.add(attached);
            }
        }
        return trackedItemMapper.toResponseList(allItems);
    }

    public void confirmWithItems(Long saleId, List<Long> itemIds) {
        itemRepo.detachFromSale(saleId);
        if (!itemIds.isEmpty()) {
            itemRepo.attachToSale(itemIds, saleId);
        }
        saleRepo.updateStatus(saleId, SaleStatus.CONFIRMED.name());
    }

    public void updateStatus(Long saleId, SaleAction action) {
        switch (action) {
            case IGNORE -> changeStatus(saleId, SaleStatus.IGNORED.name(), "");
            case VINCE -> changeStatus(saleId, SaleStatus.IGNORED.name(), "vince");
            case UNSTAGE -> changeStatus(saleId, SaleStatus.STAGED.name(), "");
        }
    }

    public void updateAmounts(Long saleId, double grossAmount, double netAmount) {
        saleRepo.updateAmounts(saleId, grossAmount, netAmount);
    }

    private void changeStatus(Long saleId, String status, String attributedTo) {
        itemRepo.detachFromSale(saleId);
        saleRepo.updateStatusAndAttribution(saleId, status, attributedTo);
    }

    public void delete(Long saleId) {
        itemRepo.detachFromSale(saleId);
        saleRepo.deleteById(saleId);
    }

}
