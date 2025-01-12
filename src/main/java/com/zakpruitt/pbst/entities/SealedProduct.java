package com.zakpruitt.pbst.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SealedProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;
    private LocalDate purchaseDate;
    private int quantity;
    private double pricePerUnit;
    private int quantityRipped;

    @OneToMany(mappedBy = "sealedProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SingleCard> singles;

    @Transient
    public double getTotalPriceSpent() {
        return quantity * pricePerUnit;
    }

    @Transient
    public boolean isRipped() {
        return quantityRipped > 0;
    }
}

