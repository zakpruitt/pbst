package com.collectingwithzak.service;

import com.collectingwithzak.dto.response.*;
import com.collectingwithzak.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final int TIMELINE_MONTHS = 12;
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final LotPurchaseRepository lotRepo;
    private final SaleRepository saleRepo;
    private final TrackedItemRepository itemRepo;
    private final GradingSubmissionRepository gradingRepo;

    public DashboardData getDashboardData() {
        double totalSpent = lotRepo.getTotalCostNonRejected();
        double totalGross = saleRepo.getTotalGross();
        double totalNet = saleRepo.getTotalNet();
        double totalFees = saleRepo.getTotalFees();
        long salesCount = saleRepo.countByStatus("CONFIRMED");
        long gradingCount = itemRepo.countByPurpose("IN_GRADING");
        long inventoryCount = itemRepo.countByPurpose("INVENTORY");
        double avgSale = salesCount > 0 ? totalNet / salesCount : 0;
        double margin = totalGross > 0 ? (totalNet / totalGross) * 100 : 0;

        double[] invTotals = itemRepo.getInventoryTotals();

        RangeTotals totals7 = saleRepo.getTotalsSince(LocalDate.now().minusDays(7));
        RangeTotals totals30 = saleRepo.getTotalsSince(LocalDate.now().minusDays(30));
        RangeTotals vinceTotals = saleRepo.getVinceTotals();

        List<MonthlySpend> spendData = lotRepo.getMonthlySpend(TIMELINE_MONTHS);
        List<MonthlyRevenue> revenueData = saleRepo.getMonthlyRevenue(TIMELINE_MONTHS);

        List<String> monthLabels = buildMonthLabels();
        List<Double> monthlySpend = fillSeries(monthLabels, spendData.stream().collect(Collectors.toMap(MonthlySpend::getMonth, MonthlySpend::getSpend)));
        List<Double> monthlyGross = fillSeries(monthLabels, revenueData.stream().collect(Collectors.toMap(MonthlyRevenue::getMonth, MonthlyRevenue::getGross)));
        List<Double> monthlyNet = fillSeries(monthLabels, revenueData.stream().collect(Collectors.toMap(MonthlyRevenue::getMonth, MonthlyRevenue::getNet)));

        return DashboardData.builder()
                .totalSpent(totalSpent)
                .totalGross(totalGross)
                .totalNet(totalNet)
                .totalFees(totalFees)
                .margin(margin)
                .salesCount(salesCount)
                .gradingCount(gradingCount)
                .inventoryCount(inventoryCount)
                .avgSale(avgSale)
                .inventoryCost(invTotals[0])
                .inventoryMarket(invTotals[1])
                .totals7(totals7)
                .totals30(totals30)
                .monthLabels(monthLabels)
                .monthlySpend(monthlySpend)
                .monthlyGross(monthlyGross)
                .monthlyNet(monthlyNet)
                .originCounts(saleRepo.countByOrigin())
                .itemTypeCounts(itemRepo.countByItemType())
                .gradingStatuses(gradingRepo.countByStatus())
                .lotStatuses(lotRepo.countByStatus())
                .topSales(saleRepo.findTopByNet(PageRequest.of(0, 5)))
                .recentSales(saleRepo.findRecent(PageRequest.of(0, 5)))
                .recentLots(lotRepo.findByOrderByPurchaseDateDesc(PageRequest.of(0, 5)))
                .vinceTotals(vinceTotals)
                .build();
    }

    private List<String> buildMonthLabels() {
        List<String> labels = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = TIMELINE_MONTHS - 1; i >= 0; i--) {
            labels.add(current.minusMonths(i).format(MONTH_FMT));
        }
        return labels;
    }

    private List<Double> fillSeries(List<String> labels, Map<String, Double> data) {
        return labels.stream()
                .map(label -> data.getOrDefault(label, 0.0))
                .toList();
    }
}
