package com.zakpruitt.collectingwithzak.entity;

import com.zakpruitt.collectingwithzak.entity.enums.PaymentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "vince_payments")
@Getter
@Setter
public class VincePayment extends BaseEntity {

    @Column(columnDefinition = "numeric(10,2)")
    private double amount;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    private String description = "";

    @Enumerated(EnumType.STRING)
    private PaymentType type = PaymentType.PAYOUT;
}
