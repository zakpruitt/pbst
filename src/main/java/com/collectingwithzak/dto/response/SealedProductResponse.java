package com.collectingwithzak.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SealedProductResponse {
    private String id;
    private String name;
    private String setName;
    private String imageUrl;
    private double marketPrice;
    private double lowPrice;
}
