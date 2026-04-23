package com.collectingwithzak.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.collectingwithzak.dto.SnapshotItem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lot_purchases")
@Getter
@Setter
@SQLDelete(sql = "UPDATE lot_purchases SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class LotPurchase extends BaseEntity {

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

    private String status = "PENDING";

    @OneToMany(mappedBy = "lotPurchase", fetch = FetchType.LAZY)
    private List<TrackedItem> trackedItems = new ArrayList<>();

    public List<SnapshotItem> parseSnapshot() {
        if (lotContentSnapshot == null || lotContentSnapshot.isBlank()) {
            return List.of();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(lotContentSnapshot, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse lot snapshot", e);
        }
    }
}
