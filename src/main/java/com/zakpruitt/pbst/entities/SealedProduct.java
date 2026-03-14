package com.zakpruitt.pbst.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

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
    @JsonManagedReference
    @ToString.Exclude
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

