package com.zakpruitt.collectingwithzak.dto.request;

import com.zakpruitt.collectingwithzak.entity.enums.PaymentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateVincePaymentRequest {
    @Positive
    private double amount;
    @NotNull
    private LocalDate paymentDate = LocalDate.now();
    private String description;
    @NotNull
    private String type = PaymentType.PAYOUT.name();
}
