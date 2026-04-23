package com.collectingwithzak.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.collectingwithzak.dto.request.CreateInventoryRequest;
import com.collectingwithzak.dto.request.UpdateInventoryRequest;
import com.collectingwithzak.dto.response.InventorySplitResponse;
import com.collectingwithzak.dto.response.TrackedItemResponse;
import com.collectingwithzak.entity.GradedDetails;
import com.collectingwithzak.entity.TrackedItem;
import com.collectingwithzak.entity.enums.ItemType;
import com.collectingwithzak.entity.enums.Purpose;
import com.collectingwithzak.exception.ResourceNotFoundException;
import com.collectingwithzak.mapper.TrackedItemMapper;
import com.collectingwithzak.repository.TrackedItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryService {

    private final TrackedItemRepository itemRepo;
    private final TrackedItemMapper trackedItemMapper;
    private final ObjectMapper objectMapper;

    public void createItems(CreateInventoryRequest request) {
        List<Map<String, Object>> rows;
        try {
            rows = objectMapper.readValue(request.getItemsSnapshot(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid items snapshot JSON", e);
        }

        String purpose = request.getPurpose() != null ? request.getPurpose() : Purpose.INVENTORY.name();

        for (Map<String, Object> row : rows) {
            TrackedItem item = new TrackedItem();
            item.setPurpose(purpose);
            item.setAcquisitionDate(request.getAcquisitionDate());
            item.setItemType((String) row.getOrDefault("item_type", ItemType.RAW_CARD.name()));

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
    public TrackedItemResponse getById(Long id) {
        TrackedItem item = findById(id);
        return trackedItemMapper.toResponse(item);
    }

    @Transactional(readOnly = true)
    public String getItemPurpose(Long id) {
        return findById(id).getPurpose();
    }

    @Transactional(readOnly = true)
    public InventorySplitResponse getByPurpose(String purpose) {
        List<TrackedItem> items = itemRepo.findByPurpose(purpose);
        List<TrackedItemResponse> all = trackedItemMapper.toResponseList(items);

        List<TrackedItemResponse> raw = new ArrayList<>();
        List<TrackedItemResponse> graded = new ArrayList<>();
        List<TrackedItemResponse> sealed = new ArrayList<>();
        List<TrackedItemResponse> other = new ArrayList<>();

        for (TrackedItemResponse item : all) {
            switch (item.getItemType()) {
                case "SEALED_PRODUCT" -> sealed.add(item);
                case "GRADED_CARD" -> graded.add(item);
                case "OTHER" -> other.add(item);
                default -> raw.add(item);
            }
        }

        return new InventorySplitResponse(all, raw, graded, sealed, other);
    }

    public String update(Long id, UpdateInventoryRequest request) {
        TrackedItem item = findById(id);

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

        if (ItemType.GRADED_CARD.name().equals(item.getItemType())) {
            if (item.getGradedDetails() == null) {
                item.setGradedDetails(new GradedDetails());
            }
            item.getGradedDetails().setGradingCompany(request.getGradingCompany());
            item.getGradedDetails().setGrade(request.getGrade());
        }

        itemRepo.save(item);
        return item.getPurpose();
    }

    public void delete(Long id) {
        itemRepo.deleteById(id);
    }

    private TrackedItem findById(Long id) {
        return itemRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TrackedItem", id));
    }
}
