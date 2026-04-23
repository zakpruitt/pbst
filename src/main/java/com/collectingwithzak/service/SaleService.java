package com.collectingwithzak.service;

import com.collectingwithzak.dto.ebay.EbayOrderData;
import com.collectingwithzak.dto.request.CreateSaleRequest;
import com.collectingwithzak.dto.response.SaleConfirmFormData;
import com.collectingwithzak.dto.response.SaleResponse;
import com.collectingwithzak.dto.response.TrackedItemResponse;
import com.collectingwithzak.entity.Sale;
import com.collectingwithzak.entity.TrackedItem;
import com.collectingwithzak.entity.enums.ItemType;
import com.collectingwithzak.entity.enums.Origin;
import com.collectingwithzak.entity.enums.SaleStatus;
import com.collectingwithzak.exception.ResourceNotFoundException;
import com.collectingwithzak.mapper.SaleMapper;
import com.collectingwithzak.mapper.TrackedItemMapper;
import com.collectingwithzak.repository.SaleRepository;
import com.collectingwithzak.repository.TrackedItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SaleService {

    private final SaleRepository saleRepo;
    private final TrackedItemRepository itemRepo;
    private final SaleMapper saleMapper;
    private final TrackedItemMapper trackedItemMapper;

    @Autowired
    @Lazy
    private SaleService self;

    public void create(CreateSaleRequest request) {
        Sale sale = saleMapper.toEntity(request);
        if (!StringUtils.hasText(sale.getOrigin())) {
            sale.setOrigin(Origin.EBAY.name());
        }
        saleRepo.save(sale);
    }

    @Transactional(readOnly = true)
    public SaleResponse getByIdWithItems(Long id) {
        Sale sale = saleRepo.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", id));
        return saleMapper.toResponse(sale);
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> getAll(String view) {
        List<Sale> sales = switch (view) {
            case "vince" -> saleRepo.findVince();
            case "ignored" -> saleRepo.findIgnored();
            default -> saleRepo.findConfirmed();
        };
        return saleMapper.toResponseList(sales);
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> getStaged() {
        return saleMapper.toResponseList(saleRepo.findByStatusOrderBySaleDateDesc(SaleStatus.STAGED.name()));
    }

    @Transactional(readOnly = true)
    public long countStaged() {
        return saleRepo.countByStatus(SaleStatus.STAGED.name());
    }

    @Transactional(readOnly = true)
    public SaleConfirmFormData getConfirmFormData(Long id) {
        Sale sale = saleRepo.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", id));

        Set<Long> attachedIds = sale.getItems().stream()
                .map(TrackedItem::getId)
                .collect(Collectors.toSet());

        List<TrackedItem> available = itemRepo.findAvailableInventory();
        List<TrackedItem> allItems = new ArrayList<>(available);
        for (TrackedItem attached : sale.getItems()) {
            if (!allItems.contains(attached)) {
                allItems.add(attached);
            }
        }

        List<TrackedItemResponse> all = trackedItemMapper.toResponseList(allItems);

        return new SaleConfirmFormData(
                saleMapper.toResponse(sale),
                TrackedItemResponse.filterByType(all, ItemType.RAW_CARD),
                TrackedItemResponse.filterByType(all, ItemType.GRADED_CARD),
                attachedIds);
    }

    public void confirmWithItems(Long saleId, List<Long> itemIds) {
        itemRepo.detachFromSale(saleId);
        if (itemIds != null && !itemIds.isEmpty()) {
            itemRepo.attachToSale(itemIds, saleId);
        }
        saleRepo.updateStatus(saleId, SaleStatus.CONFIRMED.name());
    }

    public void ignore(Long saleId) {
        itemRepo.detachFromSale(saleId);
        saleRepo.updateStatusAndAttribution(saleId, SaleStatus.IGNORED.name(), "");
    }

    public void markAsVince(Long saleId) {
        itemRepo.detachFromSale(saleId);
        saleRepo.updateStatusAndAttribution(saleId, SaleStatus.IGNORED.name(), "vince");
    }

    public void unstage(Long saleId) {
        itemRepo.detachFromSale(saleId);
        saleRepo.updateStatusAndAttribution(saleId, SaleStatus.STAGED.name(), "");
    }

    public void delete(Long saleId) {
        itemRepo.detachFromSale(saleId);
        saleRepo.deleteById(saleId);
    }

    public void syncFromEbay(List<EbayOrderData> orders) {
        int upserted = 0;
        for (EbayOrderData order : orders) {
            try {
                self.upsertFromEbay(saleMapper.fromEbayOrder(order));
                upserted++;
            } catch (Exception e) {
                log.warn("Skipping order {}: {}", order.getEbayOrderId(), e.getMessage());
            }
        }
        log.info("eBay sync: {} orders, {} upserted", orders.size(), upserted);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void upsertFromEbay(Sale sale) {
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
            sale.setStatus(SaleStatus.STAGED.name());
            saleRepo.save(sale);
        }
    }

}
