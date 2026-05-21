package com.collectingwithzak.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VincePaymentResponse {
    private Long id;
    private double amount;
    private LocalDate paymentDate;
    private String description;
    private String type;
}
