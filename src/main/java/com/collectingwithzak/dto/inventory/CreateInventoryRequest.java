package com.collectingwithzak.dto.inventory;

import com.collectingwithzak.entity.enums.Purpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateInventoryRequest {
    @NotBlank
    private String itemsSnapshot;
    @NotBlank
    private String purpose = Purpose.INVENTORY.name();
    @NotNull
    private LocalDate acquisitionDate;
}
