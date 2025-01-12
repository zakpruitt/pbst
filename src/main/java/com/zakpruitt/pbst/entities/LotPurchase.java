package com.zakpruitt.pbst.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LotPurchase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate purchaseDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    private BigDecimal totalCost; // What you actually spent
    private BigDecimal estimatedMarketValue; // Optional: helps you track your 60-80% buy-in metric

    @Column(columnDefinition = "TEXT")
    private String lotContentSnapshot; // JSON or text dump of the entire lot content at purchase time

    private String status; // PENDING, ACCEPTED, REJECTED

    @OneToMany(mappedBy = "lotPurchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrackedItem> trackedItems = new ArrayList<>();
}