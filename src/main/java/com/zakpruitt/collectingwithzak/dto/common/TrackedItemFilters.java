package com.zakpruitt.collectingwithzak.dto.common;

import com.zakpruitt.collectingwithzak.entity.TrackedItem;
import com.zakpruitt.collectingwithzak.entity.enums.ItemType;

import java.util.List;

public final class TrackedItemFilters {

    private TrackedItemFilters() {
    }

    public static List<TrackedItem> filterByType(List<TrackedItem> items, ItemType type) {
        return items.stream()
                    .filter(i -> i.getItemType() == type)
                    .toList();
    }

    public static double sumCost(List<TrackedItem> items) {
        return items.stream().mapToDouble(TrackedItem::getTotalCostBasis).sum();
    }

    public static double sumMarket(List<TrackedItem> items) {
        return items.stream().mapToDouble(TrackedItem::getEffectiveMarketValue).sum();
    }
}
