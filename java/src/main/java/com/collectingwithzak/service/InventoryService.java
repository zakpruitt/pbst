package com.collectingwithzak.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.collectingwithzak.dto.request.CreateInventoryRequest;
import com.collectingwithzak.dto.request.UpdateInventoryRequest;
import com.collectingwithzak.entity.GradedDetails;
import com.collectingwithzak.entity.Sale;
import com.collectingwithzak.entity.TrackedItem;
import com.collectingwithzak.exception.ResourceNotFoundException;
import com.collectingwithzak.repository.TrackedItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryService {

    private final TrackedItemRepository itemRepo;
    private final ObjectMapper objectMapper;

    public void createItems(CreateInventoryRequest request) {
        List<Map<String, Object>> rows;
        try {
            rows = objectMapper.readValue(request.getItemsSnapshot(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid items snapshot JSON", e);
        }

        String purpose = request.getPurpose() != null ? request.getPurpose() : "INVENTORY";

        for (Map<String, Object> row : rows) {
            TrackedItem item = new TrackedItem();
            item.setPurpose(purpose);
            item.setAcquisitionDate(request.getAcquisitionDate());
            item.setItemType((String) row.getOrDefault("item_type", "RAW_CARD"));

            String name = (String) row.get("name");
            if (name != null && !name.isBlank()) {
                item.setManualNameOverride(name);
            }

            Number cost = (Number) row.get("cost_basis");
            if (cost != null) item.setCostBasis(cost.doubleValue());

            Number market = (Number) row.get("market_price");
            if (market != null) item.setMarketValueAtPurchase(market.doubleValue());

            itemRepo.save(item);
        }
    }

    @Transactional(readOnly = true)
    public TrackedItem getById(Long id) {
        return itemRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TrackedItem", id));
    }

    @Transactional(readOnly = true)
    public List<TrackedItem> getByPurpose(String purpose) {
        return itemRepo.findByPurpose(purpose);
    }

    @Transactional(readOnly = true)
    public List<TrackedItem> getInventoryForSaleConfirm(Sale sale) {
        Set<Long> attachedIds = sale.getItems().stream()
                .map(TrackedItem::getId)
                .collect(Collectors.toSet());

        List<TrackedItem> available = itemRepo.findAvailableInventory();
        List<TrackedItem> result = new ArrayList<>(available);

        for (TrackedItem attached : sale.getItems()) {
            if (!attachedIds.isEmpty() && !result.contains(attached)) {
                result.add(attached);
            }
        }

        return result;
    }

    public TrackedItem update(Long id, UpdateInventoryRequest request) {
        TrackedItem item = getById(id);

        if (request.getName() != null && !request.getName().isBlank()) {
            item.setManualNameOverride(request.getName());
        }
        item.setCostBasis(request.getCostBasis());
        item.setMarketValueAtPurchase(request.getMarketValue());
        if (request.getAcquisitionDate() != null) {
            item.setAcquisitionDate(request.getAcquisitionDate());
        }
        item.setNotes(request.getNotes());
        if (request.getPurpose() != null && !request.getPurpose().isBlank()) {
            item.setPurpose(request.getPurpose());
        }

        if ("GRADED_CARD".equals(item.getItemType())) {
            if (item.getGradedDetails() == null) {
                item.setGradedDetails(new GradedDetails());
            }
            item.getGradedDetails().setGradingCompany(request.getGradingCompany());
            item.getGradedDetails().setGrade(request.getGrade());
        }

        return itemRepo.save(item);
    }

    public void delete(Long id) {
        itemRepo.deleteById(id);
    }
}
