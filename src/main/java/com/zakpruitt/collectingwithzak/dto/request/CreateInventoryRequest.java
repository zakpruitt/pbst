package com.zakpruitt.collectingwithzak.dto.request;

import com.zakpruitt.collectingwithzak.entity.enums.Purpose;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateInventoryRequest {
    @Valid
    @NotEmpty
    private List<InventoryItemRow> items;
    @NotBlank
    private String purpose = Purpose.INVENTORY.name();
    @NotNull
    private LocalDate acquisitionDate;
}
