package com.zakpruitt.collectingwithzak.dto.render;

import com.zakpruitt.collectingwithzak.dto.common.VinceLedger;
import com.zakpruitt.collectingwithzak.entity.LotPurchase;
import com.zakpruitt.collectingwithzak.entity.Sale;
import com.zakpruitt.collectingwithzak.repository.LabeledStat;
import com.zakpruitt.collectingwithzak.repository.RangeTotals;
import lombok.Builder;

import java.util.List;

@Builder
public record DashboardData(
        double totalSpent,
        double totalGross,
        double totalNet,
        double totalFees,
        double margin,
        long salesCount,
        long gradingCount,
        long inventoryCount,
        double avgSale,
        double inventoryMarket,
        RangeTotals totals7,
        RangeTotals totals30,
        List<String> monthLabels,
        List<Double> monthlySpend,
        List<Double> monthlyGross,
        List<Double> monthlyNet,
        List<LabeledStat> originCounts,
        List<LabeledStat> itemTypeCounts,
        List<LabeledStat> gradingStatuses,
        List<Sale> topSales,
        List<Sale> recentSales,
        List<LotPurchase> recentLots,
        VinceLedger vinceLedger) {
}
