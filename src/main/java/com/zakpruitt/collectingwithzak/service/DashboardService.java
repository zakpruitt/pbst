package com.zakpruitt.collectingwithzak.service;

import com.zakpruitt.collectingwithzak.dto.common.*;
import com.zakpruitt.collectingwithzak.dto.render.DashboardData;
import com.zakpruitt.collectingwithzak.entity.enums.ItemStatus;
import com.zakpruitt.collectingwithzak.entity.enums.SaleStatus;
import com.zakpruitt.collectingwithzak.repository.*;
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
    private static final int TOP_N = 5;
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final LotPurchaseRepository lotRepo;
    private final SaleRepository saleRepo;
    private final TrackedItemRepository itemRepo;
    private final GradingSubmissionRepository gradingRepo;
    private final VincePaymentRepository paymentRepo;

    public DashboardData getDashboardData() {
        RangeTotals confirmed = saleRepo.getConfirmedTotals();
        double totalSpent = lotRepo.getTotalCostNonRejected();
        InventoryTotals invTotals = itemRepo.getInventoryTotals();

        List<String> monthLabels = buildMonthLabels();
        List<MonthlyRevenueRow> revenue = saleRepo.getMonthlyRevenue(TIMELINE_MONTHS);
        Map<String, Double> grossByMonth = revenue.stream()
                .collect(Collectors.toMap(MonthlyRevenueRow::getMonth, MonthlyRevenueRow::getGross));
        Map<String, Double> netByMonth = revenue.stream()
                .collect(Collectors.toMap(MonthlyRevenueRow::getMonth, MonthlyRevenueRow::getNet));
        Map<String, Double> spendByMonth = lotRepo.getMonthlySpend(TIMELINE_MONTHS).stream()
                .collect(Collectors.toMap(MonthlySpendRow::getMonth, MonthlySpendRow::getSpend));

        return DashboardData.builder()
                .totalSpent(totalSpent)
                .totalGross(confirmed.gross())
                .totalNet(confirmed.net())
                .totalFees(confirmed.fees())
                .margin(confirmed.net() - totalSpent)
                .salesCount(confirmed.count())
                .avgSale(confirmed.count() > 0 ? confirmed.net() / confirmed.count() : 0)
                .gradingCount(itemRepo.countByStatus(ItemStatus.IN_GRADING))
                .inventoryCount(itemRepo.countByStatus(ItemStatus.AVAILABLE))
                .inventoryMarket(invTotals.market())
                .totals7(saleRepo.getTotalsSince(LocalDate.now().minusDays(7)))
                .totals30(saleRepo.getTotalsSince(LocalDate.now().minusDays(30)))
                .monthLabels(monthLabels)
                .monthlySpend(fillSeries(monthLabels, spendByMonth))
                .monthlyGross(fillSeries(monthLabels, grossByMonth))
                .monthlyNet(fillSeries(monthLabels, netByMonth))
                .originCounts(saleRepo.countByOrigin())
                .itemTypeCounts(itemRepo.countByItemType())
                .gradingStatuses(gradingRepo.countByStatus())
                .topSales(saleRepo.findByStatusOrderByNetAmountDesc(SaleStatus.CONFIRMED, PageRequest.of(0, TOP_N)))
                .recentSales(saleRepo.findByStatusOrderBySaleDateDesc(SaleStatus.CONFIRMED, PageRequest.of(0, TOP_N)))
                .recentLots(lotRepo.findByOrderByPurchaseDateDesc(PageRequest.of(0, TOP_N)))
                .vinceLedger(VinceLedger.from(saleRepo.getVinceTotals(), paymentRepo.getTotals()))
                .build();
    }

    private List<String> buildMonthLabels() {
        List<String> labels = new ArrayList<>(TIMELINE_MONTHS);
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
