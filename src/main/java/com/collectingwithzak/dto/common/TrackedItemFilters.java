package com.collectingwithzak.dto.common;

import com.collectingwithzak.dto.response.TrackedItemResponse;
import com.collectingwithzak.entity.enums.ItemType;

import java.util.List;

public final class TrackedItemFilters {

    private TrackedItemFilters() {
    }

    public static List<TrackedItemResponse> filterByType(List<TrackedItemResponse> items, ItemType type) {
        return items.stream()
                .filter(i -> type.name().equals(i.getItemType()))
                .toList();
    }

    public static double sumCost(List<TrackedItemResponse> items) {
        return items.stream().mapToDouble(TrackedItemResponse::getTotalCostBasis).sum();
    }

    public static double sumMarket(List<TrackedItemResponse> items) {
        return items.stream().mapToDouble(TrackedItemResponse::getMarketValueAtPurchase).sum();
    }
}
