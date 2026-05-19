package com.collectingwithzak.service;

import com.collectingwithzak.dto.inventory.CreateInventoryRequest;
import com.collectingwithzak.dto.inventory.InventoryIndexData;
import com.collectingwithzak.dto.inventory.InventoryItemRow;
import com.collectingwithzak.dto.inventory.TrackedItemFilters;
import com.collectingwithzak.dto.inventory.TrackedItemResponse;
import com.collectingwithzak.dto.inventory.UpdateInventoryRequest;
import com.collectingwithzak.entity.GradedDetails;
import com.collectingwithzak.entity.TrackedItem;
import com.collectingwithzak.entity.enums.ItemType;
import com.collectingwithzak.entity.enums.ItemStatus;
import com.collectingwithzak.exception.ResourceNotFoundException;
import com.collectingwithzak.mapper.GradedDetailsMapper;
import com.collectingwithzak.mapper.TrackedItemMapper;
import com.collectingwithzak.repository.PokemonCardRepository;
import com.collectingwithzak.repository.SealedProductRepository;
import com.collectingwithzak.repository.TrackedItemRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryService {

    private final TrackedItemRepository itemRepo;
    private final PokemonCardRepository cardRepo;
    private final SealedProductRepository sealedRepo;
    private final TrackedItemMapper trackedItemMapper;
    private final GradedDetailsMapper gradedDetailsMapper;
    private final ObjectMapper objectMapper;

    public InventoryIndexData getIndexData(String tab) {
        List<TrackedItemResponse> allItems = getItemsForTab(tab);
        return InventoryIndexData.builder()
                .items(allItems)
                .rawItems(TrackedItemFilters.filterByType(allItems, ItemType.RAW_CARD))
                .gradedItems(TrackedItemFilters.filterByType(allItems, ItemType.GRADED_CARD))
                .sealedItems(TrackedItemFilters.filterByType(allItems, ItemType.SEALED_PRODUCT))
                .otherItems(TrackedItemFilters.filterByType(allItems, ItemType.OTHER))
                .purpose(tab)
                .totalCost(TrackedItemFilters.sumCost(allItems))
                .totalMarket(TrackedItemFilters.sumMarket(allItems))
                .build();
    }

    public void createItems(CreateInventoryRequest request) {
        List<InventoryItemRow> rows = parseSnapshot(request.getItemsSnapshot());

        for (InventoryItemRow row : rows) {
            TrackedItem item = trackedItemMapper.fromSnapshotRow(row, request);
            linkAssociations(item, row);
            itemRepo.save(item);
        }
    }

    public TrackedItemResponse getById(Long id) {
        TrackedItem item = findById(id);
        return trackedItemMapper.toResponse(item);
    }

    public List<TrackedItemResponse> getItemsForTab(String tab) {
        if (ItemStatus.IN_GRADING.name().equals(tab)) {
            List<TrackedItem> items = itemRepo.findByStatus(ItemStatus.IN_GRADING.name());
            return trackedItemMapper.toResponseList(items);
        }
        List<TrackedItem> items = itemRepo.findByPurpose(tab);
        return trackedItemMapper.toResponseList(items);
    }

    public String update(Long id, UpdateInventoryRequest request) {
        TrackedItem item = findById(id);
        trackedItemMapper.updateEntity(request, item);

        if (ItemType.GRADED_CARD.name().equals(item.getItemType())) {
            updateGradedDetails(item, request);
        }

        itemRepo.save(item);
        return item.getPurpose();
    }

    public String delete(Long id) {
        TrackedItem item = findById(id);
        String purpose = item.getPurpose();
        item.setStatus(ItemStatus.ARCHIVED.name());
        itemRepo.save(item);
        return purpose;
    }

    private TrackedItem findById(Long id) {
        return itemRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TrackedItem", id));
    }

    private List<InventoryItemRow> parseSnapshot(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid items snapshot JSON", e);
        }
    }

    private void linkAssociations(TrackedItem item, InventoryItemRow row) {
        if (!row.getPokemonCardId().isEmpty()) {
            item.setPokemonCard(cardRepo.findById(row.getPokemonCardId()).orElse(null));
        }
        if (!row.getSealedProductId().isEmpty()) {
            item.setSealedProduct(sealedRepo.findById(row.getSealedProductId()).orElse(null));
        }
    }

    private void updateGradedDetails(TrackedItem item, UpdateInventoryRequest request) {
        double existingUpcharge = item.getGradedDetails() != null
                ? item.getGradedDetails().getGradingUpcharge() : 0;
        GradedDetails details = gradedDetailsMapper.fromUpdateRequest(request);
        details.setGradingUpcharge(existingUpcharge);
        item.setGradedDetails(details);
    }
}
