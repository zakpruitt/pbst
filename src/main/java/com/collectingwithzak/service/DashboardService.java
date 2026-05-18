package com.collectingwithzak.service;

import com.collectingwithzak.dto.response.*;
import com.collectingwithzak.entity.enums.Purpose;
import com.collectingwithzak.mapper.LotMapper;
import com.collectingwithzak.mapper.SaleMapper;
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
    private static final int TOP_N = 5;
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final LotPurchaseRepository lotRepo;
    private final SaleRepository saleRepo;
    private final TrackedItemRepository itemRepo;
    private final GradingSubmissionRepository gradingRepo;
    private final SaleMapper saleMapper;
    private final LotMapper lotMapper;
    private final VincePaymentService vincePaymentService;

    public DashboardData getDashboardData() {
        ConfirmedSaleTotals confirmed = saleRepo.getConfirmedTotals();
        double totalSpent = lotRepo.getTotalCostNonRejected();
        InventoryTotals invTotals = itemRepo.getInventoryTotals();

        List<String> monthLabels = buildMonthLabels();
        List<MonthlyRevenue> revenueData = saleRepo.getMonthlyRevenue(TIMELINE_MONTHS);
        Map<String, Double> grossByMonth = revenueData.stream().collect(Collectors.toMap(MonthlyRevenue::getMonth, MonthlyRevenue::getGross));
        Map<String, Double> netByMonth = revenueData.stream().collect(Collectors.toMap(MonthlyRevenue::getMonth, MonthlyRevenue::getNet));

        return DashboardData.builder()
                .totalSpent(totalSpent)
                .totalGross(confirmed.getGross())
                .totalNet(confirmed.getNet())
                .totalFees(confirmed.getFees())
                .margin(confirmed.getNet() - totalSpent)
                .salesCount(confirmed.getCount())
                .avgSale(confirmed.getCount() > 0 ? confirmed.getNet() / confirmed.getCount() : 0)
                .gradingCount(itemRepo.countByPurpose(Purpose.IN_GRADING.name()))
                .inventoryCount(itemRepo.countByPurpose(Purpose.INVENTORY.name()))
                .inventoryCost(invTotals.getCost())
                .inventoryMarket(invTotals.getMarket())
                .totals7(saleRepo.getTotalsSince(LocalDate.now().minusDays(7)))
                .totals30(saleRepo.getTotalsSince(LocalDate.now().minusDays(30)))
                .monthLabels(monthLabels)
                .monthlySpend(buildSpendSeries(monthLabels))
                .monthlyGross(fillSeries(monthLabels, grossByMonth))
                .monthlyNet(fillSeries(monthLabels, netByMonth))
                .originCounts(saleRepo.countByOrigin())
                .itemTypeCounts(itemRepo.countByItemType())
                .gradingStatuses(gradingRepo.countByStatus())
                .lotStatuses(lotRepo.countByStatus())
                .topSales(saleMapper.toResponseList(saleRepo.findTopByNet(PageRequest.of(0, TOP_N))))
                .recentSales(saleMapper.toResponseList(saleRepo.findRecent(PageRequest.of(0, TOP_N))))
                .recentLots(lotMapper.toResponseList(lotRepo.findByOrderByPurchaseDateDesc(PageRequest.of(0, TOP_N))))
                .vinceLedger(vincePaymentService.getLedger())
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

    private List<Double> buildSpendSeries(List<String> monthLabels) {
        Map<String, Double> data = lotRepo.getMonthlySpend(TIMELINE_MONTHS).stream()
                .collect(Collectors.toMap(MonthlySpend::getMonth, MonthlySpend::getSpend));
        return fillSeries(monthLabels, data);
    }

    private List<Double> fillSeries(List<String> labels, Map<String, Double> data) {
        return labels.stream()
                .map(label -> data.getOrDefault(label, 0.0))
                .toList();
    }
}
