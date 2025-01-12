package com.zakpruitt.pbst.entities;

import com.zakpruitt.pbst.enums.ProductType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sellerName;
    private LocalDate saleDate;
    private double salePrice;
    private double profit;

    @Enumerated(EnumType.STRING)
    private ProductType productType;

    private Long productId; // polymorphic association between both products
}
