package com.collectingwithzak.dto.vince;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VincePaymentTotals {
    private double paidOut;
    private double vinceOwes;
}
