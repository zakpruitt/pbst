package com.zakpruitt.collectingwithzak.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "expenses")
@Getter
@Setter
public class Expense extends BaseEntity {

    private String name;

    @Column(name = "expense_date")
    private LocalDate expenseDate;

    @Column(columnDefinition = "numeric(10,2)")
    private double cost;
}
