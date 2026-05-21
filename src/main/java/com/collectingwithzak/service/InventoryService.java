package com.collectingwithzak.service;

import com.collectingwithzak.dto.request.CreateInventoryRequest;
import com.collectingwithzak.dto.request.InventoryItemRow;
import com.collectingwithzak.dto.request.UpdateInventoryRequest;
import com.collectingwithzak.entity.GradedDetails;
import com.collectingwithzak.entity.TrackedItem;
import com.collectingwithzak.entity.enums.ItemType;
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

    public void createItems(CreateInventoryRequest request) {
        List<InventoryItemRow> rows = parseSnapshot(request.getItemsSnapshot());

        for (InventoryItemRow row : rows) {
            TrackedItem item = trackedItemMapper.fromSnapshotRow(row, request);
            linkAssociations(item, row);
            itemRepo.save(item);
        }
    }

    public String update(Long id, UpdateInventoryRequest request) {
        TrackedItem item = findById(id);
        trackedItemMapper.updateEntity(request, item);

        if (item.getItemType() == ItemType.GRADED_CARD) {
            if (item.getGradedDetails() == null) {
                item.setGradedDetails(new GradedDetails());
            }
            gradedDetailsMapper.updateFromRequest(request, item.getGradedDetails());
        }

        return item.getPurpose().name();
    }

    public String delete(Long id) {
        TrackedItem item = findById(id);
        String purpose = item.getPurpose().name();
        itemRepo.delete(item);
        return purpose;
    }

    private TrackedItem findById(Long id) {
        return itemRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TrackedItem", id));
    }

    private List<InventoryItemRow> parseSnapshot(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
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
}
