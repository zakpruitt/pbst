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
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final LotPurchaseRepository lotRepo;
    private final SaleRepository saleRepo;
    private final TrackedItemRepository itemRepo;
    private final GradingSubmissionRepository gradingRepo;
    private final SaleMapper saleMapper;
    private final LotMapper lotMapper;
    private final VincePaymentService vincePaymentService;

    public DashboardData getDashboardData() {
        double totalSpent = lotRepo.getTotalCostNonRejected();
        double[] confirmed = saleRepo.getConfirmedTotals();
        long salesCount = (long) confirmed[0];
        double totalGross = confirmed[1];
        double totalNet = confirmed[2];
        double totalFees = confirmed[3];
        long gradingCount = itemRepo.countByPurpose(Purpose.IN_GRADING.name());
        long inventoryCount = itemRepo.countByPurpose(Purpose.INVENTORY.name());
        double avgSale = salesCount > 0 ? totalNet / salesCount : 0;
        double margin = totalNet - totalSpent;

        double[] invTotals = itemRepo.getInventoryTotals();

        RangeTotals totals7 = saleRepo.getTotalsSince(LocalDate.now().minusDays(7));
        RangeTotals totals30 = saleRepo.getTotalsSince(LocalDate.now().minusDays(30));
        VinceLedger vinceLedger = vincePaymentService.getLedger();

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
                .topSales(saleMapper.toResponseList(saleRepo.findTopByNet(PageRequest.of(0, 5))))
                .recentSales(saleMapper.toResponseList(saleRepo.findRecent(PageRequest.of(0, 5))))
                .recentLots(lotMapper.toResponseList(lotRepo.findByOrderByPurchaseDateDesc(PageRequest.of(0, 5))))
                .vinceLedger(vinceLedger)
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
