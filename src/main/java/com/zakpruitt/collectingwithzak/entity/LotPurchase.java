package com.zakpruitt.collectingwithzak.entity;

import com.zakpruitt.collectingwithzak.dto.request.SnapshotItem;
import com.zakpruitt.collectingwithzak.entity.enums.LotStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lot_purchases")
@Getter
@Setter
public class LotPurchase extends BaseEntity {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Column(name = "seller_name")
    private String sellerName;

    private String description;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "total_cost", columnDefinition = "numeric(10,2)")
    private double totalCost;

    @Column(name = "estimated_market_value", columnDefinition = "numeric(10,2)")
    private double estimatedMarketValue;

    @Column(name = "lot_content_snapshot", columnDefinition = "text")
    private String lotContentSnapshot;

    @Enumerated(EnumType.STRING)
    private LotStatus status = LotStatus.PENDING;

    @OneToMany(mappedBy = "lotPurchase", fetch = FetchType.LAZY)
    private List<TrackedItem> trackedItems = new ArrayList<>();

    public List<SnapshotItem> parseSnapshot() {
        if (lotContentSnapshot == null || lotContentSnapshot.isBlank()) {
            return List.of();
        }
        try {
            return List.of(MAPPER.readValue(lotContentSnapshot, SnapshotItem[].class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse lot snapshot", e);
        }
    }
}
