package com.zakpruitt.collectingwithzak.dto.common;

/** Projection for the monthly revenue native query — one row per month with sales. */
public interface MonthlyRevenueRow {

    String getMonth();

    double getGross();

    double getNet();
}
