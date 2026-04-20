package com.collectingwithzak.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateInventoryRequest {
    private String itemsSnapshot;
    private String purpose;
    private LocalDate acquisitionDate;
}
