package com.zakpruitt.collectingwithzak.dto.render;

import com.zakpruitt.collectingwithzak.dto.common.TrackedItemFilters;
import com.zakpruitt.collectingwithzak.entity.TrackedItem;
import com.zakpruitt.collectingwithzak.entity.enums.ItemType;

import java.util.List;

public record InventoryIndexData(List<TrackedItem> items, String purpose) {

    public List<TrackedItem> rawItems() {
        return TrackedItemFilters.filterByType(items, ItemType.RAW_CARD);
    }

    public List<TrackedItem> gradedItems() {
        return TrackedItemFilters.filterByType(items, ItemType.GRADED_CARD);
    }

    public List<TrackedItem> sealedItems() {
        return TrackedItemFilters.filterByType(items, ItemType.SEALED_PRODUCT);
    }

    public List<TrackedItem> otherItems() {
        return TrackedItemFilters.filterByType(items, ItemType.OTHER);
    }

    public double totalCost() {
        return TrackedItemFilters.sumCost(items);
    }

    public double totalMarket() {
        return TrackedItemFilters.sumMarket(items);
    }
}
