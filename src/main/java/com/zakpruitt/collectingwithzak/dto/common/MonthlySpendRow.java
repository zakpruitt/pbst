package com.zakpruitt.collectingwithzak.dto.common;

/** Projection for the monthly lot-spend native query — one row per month with purchases. */
public interface MonthlySpendRow {

    String getMonth();

    double getSpend();
}
