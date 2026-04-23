package com.collectingwithzak.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Entity
@Table(name = "vince_payments")
@Getter
@Setter
@SQLDelete(sql = "UPDATE vince_payments SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class VincePayment extends BaseEntity {

    @Column(columnDefinition = "numeric(10,2)")
    private double amount;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    private String description = "";

    private String type = "PAYOUT";
}
