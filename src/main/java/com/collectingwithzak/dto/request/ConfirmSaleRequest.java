package com.collectingwithzak.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class ConfirmSaleRequest {
    private List<Long> itemIds;
}
