package com.zakpruitt.collectingwithzak.service;

import com.zakpruitt.collectingwithzak.dto.request.CreateInventoryRequest;
import com.zakpruitt.collectingwithzak.dto.request.InventoryItemRow;
import com.zakpruitt.collectingwithzak.dto.request.UpdateInventoryRequest;
import com.zakpruitt.collectingwithzak.entity.GradedDetails;
import com.zakpruitt.collectingwithzak.entity.TrackedItem;
import com.zakpruitt.collectingwithzak.entity.enums.ItemType;
import com.zakpruitt.collectingwithzak.exception.ResourceNotFoundException;
import com.zakpruitt.collectingwithzak.mapper.GradedDetailsMapper;
import com.zakpruitt.collectingwithzak.mapper.TrackedItemMapper;
import com.zakpruitt.collectingwithzak.repository.PokemonCardRepository;
import com.zakpruitt.collectingwithzak.repository.SealedProductRepository;
import com.zakpruitt.collectingwithzak.repository.TrackedItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryService {

    private final TrackedItemRepository itemRepo;
    private final PokemonCardRepository cardRepo;
    private final SealedProductRepository sealedRepo;
    private final TrackedItemMapper trackedItemMapper;
    private final GradedDetailsMapper gradedDetailsMapper;

    public void createItems(CreateInventoryRequest request) {
        for (InventoryItemRow row : request.getItems()) {
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

    private void linkAssociations(TrackedItem item, InventoryItemRow row) {
        if (row.getPokemonCardId() != null && !row.getPokemonCardId().isEmpty()) {
            cardRepo.findById(row.getPokemonCardId())
                    .ifPresent(item::setPokemonCard);
        }
        if (row.getSealedProductId() != null && !row.getSealedProductId().isEmpty()) {
            sealedRepo.findById(row.getSealedProductId())
                    .ifPresent(item::setSealedProduct);
        }
    }
}
