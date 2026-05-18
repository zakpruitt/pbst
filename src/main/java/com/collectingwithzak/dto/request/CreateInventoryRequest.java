package com.collectingwithzak.dto.request;

import com.collectingwithzak.entity.enums.Purpose;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateInventoryRequest {
    private String itemsSnapshot;
    private String purpose = Purpose.INVENTORY.name();
    private LocalDate acquisitionDate;
}
