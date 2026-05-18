package com.collectingwithzak.dto.request;

import com.collectingwithzak.entity.enums.PaymentType;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateVincePaymentRequest {
    private double amount;
    private LocalDate paymentDate = LocalDate.now();
    private String description;
    private String type = PaymentType.PAYOUT.name();
}
