package com.zakpruitt.collectingwithzak.service.render;

import com.zakpruitt.collectingwithzak.dto.common.InventoryTotals;
import com.zakpruitt.collectingwithzak.dto.common.MonthlyRevenueRow;
import com.zakpruitt.collectingwithzak.dto.common.MonthlySpendRow;
import com.zakpruitt.collectingwithzak.dto.common.RangeTotals;
import com.zakpruitt.collectingwithzak.dto.common.VincePaymentTotals;
import com.zakpruitt.collectingwithzak.dto.render.DashboardData;
import com.zakpruitt.collectingwithzak.dto.response.VinceLedger;
import com.zakpruitt.collectingwithzak.entity.enums.ItemStatus;
import com.zakpruitt.collectingwithzak.entity.enums.SaleStatus;
import com.zakpruitt.collectingwithzak.mapper.LotMapper;
import com.zakpruitt.collectingwithzak.mapper.SaleMapper;
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
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardRenderService {

    private static final int TIMELINE_MONTHS = 12;
    private static final int TOP_N = 5;
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final LotPurchaseRepository lotRepo;
    private final SaleRepository saleRepo;
    private final TrackedItemRepository itemRepo;
    private final GradingSubmissionRepository gradingRepo;
    private final VincePaymentRepository paymentRepo;
    private final SaleMapper saleMapper;
    private final LotMapper lotMapper;

    public DashboardData getDashboardData() {
        RangeTotals confirmed = saleRepo.getConfirmedTotals();
        double totalSpent = lotRepo.getTotalCostNonRejected();
        InventoryTotals invTotals = itemRepo.getInventoryTotals();

        List<String> monthLabels = buildMonthLabels();
        Map<String, MonthlyRevenueRow> revenueByMonth = saleRepo.getMonthlyRevenue(TIMELINE_MONTHS).stream()
                .collect(Collectors.toMap(MonthlyRevenueRow::getMonth, Function.identity()));
        Map<String, Double> spendByMonth = lotRepo.getMonthlySpend(TIMELINE_MONTHS).stream()
                .collect(Collectors.toMap(MonthlySpendRow::getMonth, MonthlySpendRow::getSpend));

        RangeTotals vinceSalesTotals = saleRepo.getVinceTotals();
        VincePaymentTotals paymentTotals = paymentRepo.getTotals();
        VinceLedger vinceLedger = VinceLedger.from(vinceSalesTotals, paymentTotals.getPaidOut(), paymentTotals.getVinceOwes());

        return DashboardData.builder()
                .totalSpent(totalSpent)
                .totalGross(confirmed.getGross())
                .totalNet(confirmed.getNet())
                .totalFees(confirmed.getFees())
                .margin(confirmed.getNet() - totalSpent)
                .salesCount(confirmed.getCount())
                .avgSale(confirmed.getCount() > 0 ? confirmed.getNet() / confirmed.getCount() : 0)
                .gradingCount(itemRepo.countByStatus(ItemStatus.IN_GRADING))
                .inventoryCount(itemRepo.countByStatus(ItemStatus.AVAILABLE))
                .inventoryMarket(invTotals.getMarket())
                .totals7(saleRepo.getTotalsSince(LocalDate.now().minusDays(7)))
                .totals30(saleRepo.getTotalsSince(LocalDate.now().minusDays(30)))
                .monthLabels(monthLabels)
                .monthlySpend(fillSeries(monthLabels, spendByMonth))
                .monthlyGross(fillRevenueSeries(monthLabels, revenueByMonth, MonthlyRevenueRow::getGross))
                .monthlyNet(fillRevenueSeries(monthLabels, revenueByMonth, MonthlyRevenueRow::getNet))
                .originCounts(saleRepo.countByOrigin())
                .itemTypeCounts(itemRepo.countByItemType())
                .gradingStatuses(gradingRepo.countByStatus())
                .topSales(saleMapper.toResponseList(saleRepo.findByStatusOrderByNetAmountDesc(SaleStatus.CONFIRMED, PageRequest.of(0, TOP_N))))
                .recentSales(saleMapper.toResponseList(saleRepo.findByStatusOrderBySaleDateDesc(SaleStatus.CONFIRMED, PageRequest.of(0, TOP_N))))
                .recentLots(lotMapper.toResponseList(lotRepo.findByOrderByPurchaseDateDesc(PageRequest.of(0, TOP_N))))
                .vinceLedger(vinceLedger)
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

    private List<Double> fillRevenueSeries(List<String> labels, Map<String, MonthlyRevenueRow> data,
                                           ToDoubleFunction<MonthlyRevenueRow> extractor) {
        return labels.stream()
                .map(label -> {
                    MonthlyRevenueRow revenue = data.get(label);
                    return revenue != null ? extractor.applyAsDouble(revenue) : 0.0;
                })
                .toList();
    }
}
