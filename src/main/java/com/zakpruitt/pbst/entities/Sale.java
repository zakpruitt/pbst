package com.zakpruitt.pbst.entities;

import com.zakpruitt.pbst.enums.ProductType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate saleDate;
    private double salePrice;
    private double profit;

    @Enumerated(EnumType.STRING)
    private ProductType productType;

    private Long productId; // polymorphic association between both products
}
