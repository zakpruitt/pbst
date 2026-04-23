package com.collectingwithzak.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateVincePaymentRequest {
    private double amount;
    private LocalDate paymentDate;
    private String description;
    private String type;
}
