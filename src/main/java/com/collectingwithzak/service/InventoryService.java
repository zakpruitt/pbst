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
import org.springframework.util.StringUtils;
import com.collectingwithzak.exception.ResourceNotFoundException;
import com.collectingwithzak.mapper.TrackedItemMapper;
import com.collectingwithzak.repository.PokemonCardRepository;
import com.collectingwithzak.repository.SealedProductRepository;
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
    private final PokemonCardRepository cardRepo;
    private final SealedProductRepository sealedRepo;
    private final TrackedItemMapper trackedItemMapper;
    private final ObjectMapper objectMapper;

    // ---------- Create ----------

    public void createItems(CreateInventoryRequest request) {
        List<Map<String, Object>> rows;
        try {
            rows = objectMapper.readValue(request.getItemsSnapshot(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid items snapshot JSON", e);
        }

        String purpose = request.getPurpose() != null ? request.getPurpose() : Purpose.INVENTORY.name();

        for (Map<String, Object> row : rows) {
            String name = (String) row.get("name");
            Number cost = (Number) row.get("cost_basis");
            Number market = (Number) row.get("market_value");
            String cardId = (String) row.get("pokemon_card_id");
            String sealedId = (String) row.get("sealed_product_id");

            var builder = TrackedItem.builder()
                    .purpose(purpose)
                    .acquisitionDate(request.getAcquisitionDate())
                    .itemType((String) row.getOrDefault("item_type", ItemType.RAW_CARD.name()));

            if (StringUtils.hasText(name)) builder.manualNameOverride(name);
            if (cost != null) builder.costBasis(cost.doubleValue());
            if (market != null) builder.marketValueAtPurchase(market.doubleValue());
            if (StringUtils.hasText(cardId)) builder.pokemonCard(cardRepo.findById(cardId).orElse(null));
            if (StringUtils.hasText(sealedId)) builder.sealedProduct(sealedRepo.findById(sealedId).orElse(null));

            itemRepo.save(builder.build());
        }
    }

    // ---------- Read ----------

    public TrackedItemResponse getById(Long id) {
        TrackedItem item = findById(id);
        return trackedItemMapper.toResponse(item);
    }


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

    // ---------- Update ----------

    public String update(Long id, UpdateInventoryRequest request) {
        TrackedItem item = findById(id);
        trackedItemMapper.updateEntity(request, item);

        if (ItemType.GRADED_CARD.name().equals(item.getItemType())) {
            item.setGradedDetails(GradedDetails.builder()
                    .gradingCompany(request.getGradingCompany())
                    .grade(request.getGrade())
                    .gradingUpcharge(item.getGradedDetails() != null ? item.getGradedDetails().getGradingUpcharge() : 0)
                    .build());
        }

        itemRepo.save(item);
        return item.getPurpose();
    }

    // ---------- Delete ----------

    public String delete(Long id) {
        TrackedItem item = findById(id);
        String purpose = item.getPurpose();
        item.setPurpose(Purpose.ARCHIVED.name());
        itemRepo.save(item);
        return purpose;
    }

    // ---------- Helpers ----------

    private TrackedItem findById(Long id) {
        return itemRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TrackedItem", id));
    }
}
