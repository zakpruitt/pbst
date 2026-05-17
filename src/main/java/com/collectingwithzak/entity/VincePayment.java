package com.collectingwithzak.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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

    private String type = "PAYOUT";
}
