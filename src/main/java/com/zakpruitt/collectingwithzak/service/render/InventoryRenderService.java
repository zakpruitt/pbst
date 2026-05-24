package com.zakpruitt.collectingwithzak.service.render;

import com.zakpruitt.collectingwithzak.dto.common.TrackedItemFilters;
import com.zakpruitt.collectingwithzak.dto.render.InventoryIndexData;
import com.zakpruitt.collectingwithzak.dto.response.TrackedItemResponse;
import com.zakpruitt.collectingwithzak.entity.TrackedItem;
import com.zakpruitt.collectingwithzak.entity.enums.ItemStatus;
import com.zakpruitt.collectingwithzak.entity.enums.ItemType;
import com.zakpruitt.collectingwithzak.entity.enums.Purpose;
import com.zakpruitt.collectingwithzak.exception.ResourceNotFoundException;
import com.zakpruitt.collectingwithzak.mapper.TrackedItemMapper;
import com.zakpruitt.collectingwithzak.repository.TrackedItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryRenderService {

    private final TrackedItemRepository itemRepo;
    private final TrackedItemMapper trackedItemMapper;

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

    public TrackedItemResponse getById(Long id) {
        TrackedItem item = findById(id);
        return trackedItemMapper.toResponse(item);
    }

    public List<TrackedItemResponse> getItemsForTab(String tab) {
        if (ItemStatus.IN_GRADING.name().equals(tab)) {
            List<TrackedItem> items = itemRepo.findByStatus(ItemStatus.IN_GRADING);
            return trackedItemMapper.toResponseList(items);
        }
        List<TrackedItem> items = itemRepo.findByPurpose(Purpose.valueOf(tab));
        return trackedItemMapper.toResponseList(items);
    }

    private TrackedItem findById(Long id) {
        return itemRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TrackedItem", id));
    }
}
