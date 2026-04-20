package com.collectingwithzak.dto.response;

import com.collectingwithzak.entity.LotPurchase;
import com.collectingwithzak.entity.Sale;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
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

    private List<OriginCount> originCounts;
    private List<ItemTypeCount> itemTypeCounts;
    private List<GradingStatusCount> gradingStatuses;
    private List<LotStatusCount> lotStatuses;

    private List<Sale> topSales;
    private List<Sale> recentSales;
    private List<LotPurchase> recentLots;
    private RangeTotals vinceTotals;
}
