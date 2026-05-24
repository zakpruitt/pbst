package com.zakpruitt.collectingwithzak.dto.render;

import com.zakpruitt.collectingwithzak.dto.common.LabeledStat;
import com.zakpruitt.collectingwithzak.dto.common.RangeTotals;
import com.zakpruitt.collectingwithzak.dto.response.LotResponse;
import com.zakpruitt.collectingwithzak.dto.response.SaleResponse;
import com.zakpruitt.collectingwithzak.dto.response.VinceLedger;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardData {
    private double totalSpent;
    private double totalGross;
    private double totalNet;
    private double totalFees;
    private double margin;
    private long salesCount;
    private long gradingCount;
    private long inventoryCount;
    private double avgSale;
    private double inventoryCost;
    private double inventoryMarket;

    private RangeTotals totals7;
    private RangeTotals totals30;

    private List<String> monthLabels;
    private List<Double> monthlySpend;
    private List<Double> monthlyGross;
    private List<Double> monthlyNet;

    private List<LabeledStat> originCounts;
    private List<LabeledStat> itemTypeCounts;
    private List<LabeledStat> gradingStatuses;
    private List<LabeledStat> lotStatuses;

    private List<SaleResponse> topSales;
    private List<SaleResponse> recentSales;
    private List<LotResponse> recentLots;
    private VinceLedger vinceLedger;
}
