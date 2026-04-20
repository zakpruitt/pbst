package com.collectingwithzak.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyRevenue {
    private String month;
    private double gross;
    private double net;
    private long count;
}
