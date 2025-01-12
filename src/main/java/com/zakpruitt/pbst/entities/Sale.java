package com.zakpruitt.pbst.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String ebayOrderId; // Unique ID from eBay

    private LocalDate saleDate;
    private String title;
    private String buyerUsername;

    private BigDecimal grossAmount; // Total buyer paid
    private BigDecimal ebayFees;    // FVF, Ad fees, etc.
    private BigDecimal shippingCost; // Your label cost
    private BigDecimal netAmount;   // Final take-home

    private String imageUrl;
    private String orderStatus; // PAID, SHIPPED, etc.

    @Column(columnDefinition = "TEXT")
    private String notes;
}