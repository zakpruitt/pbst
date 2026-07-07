package com.zakpruitt.collectingwithzak.dto.common;

import com.zakpruitt.collectingwithzak.dto.response.TrackedItemResponse;
import com.zakpruitt.collectingwithzak.entity.enums.ItemType;

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
        return items.stream().mapToDouble(TrackedItemFilters::marketValue).sum();
    }

    // Same fallback the inventory rows display: purchase-time value, else the card's current market price.
    private static double marketValue(TrackedItemResponse item) {
        if (item.getMarketValueAtPurchase() != 0) {
            return item.getMarketValueAtPurchase();
        }
        return item.getPokemonCard() != null ? item.getPokemonCard().getMarketPrice() : 0;
    }
}
