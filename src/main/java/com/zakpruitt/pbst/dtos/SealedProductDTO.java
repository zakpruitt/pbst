package com.zakpruitt.pbst.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SealedProductDTO {

    @NotBlank(message = "Product name must not be blank")
    private String productName;

    @NotNull(message = "Purchase date must not be null")
    private LocalDate purchaseDate;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    @Positive(message = "Price per unit must be greater than zero")
    private double pricePerUnit;

    @Min(value = 0, message = "Quantity ripped cannot be negative")
    private int quantityRipped;

    //TODO: private List<SingleCardDTO> singles;
}
