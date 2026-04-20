package com.collectingwithzak.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OriginCount {
    private String origin;
    private long count;
    private double net;
}
