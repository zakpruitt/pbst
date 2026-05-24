package com.zakpruitt.collectingwithzak.service.render;

import com.zakpruitt.collectingwithzak.dto.common.InventoryTotals;
import com.zakpruitt.collectingwithzak.dto.common.MonthlyRevenue;
import com.zakpruitt.collectingwithzak.dto.common.RangeTotals;
import com.zakpruitt.collectingwithzak.dto.common.VincePaymentTotals;
import com.zakpruitt.collectingwithzak.dto.render.DashboardData;
import com.zakpruitt.collectingwithzak.dto.response.VinceLedger;
import com.zakpruitt.collectingwithzak.entity.enums.ItemStatus;
import com.zakpruitt.collectingwithzak.entity.enums.SaleStatus;
import com.zakpruitt.collectingwithzak.mapper.LotMapper;
import com.zakpruitt.collectingwithzak.mapper.SaleMapper;
import com.collectingwithzak.repository.*;
import com.zakpruitt.collectingwithzak.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
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
        Map<String, MonthlyRevenue> revenueByMonth = saleRepo.getMonthlyRevenueRaw(TIMELINE_MONTHS).stream()
                .map(row -> new MonthlyRevenue(
                        (String) row[0],
                        ((Number) row[1]).doubleValue(),
                        ((Number) row[2]).doubleValue()))
                .collect(Collectors.toMap(MonthlyRevenue::getMonth, Function.identity()));

        Map<String, Double> spendByMonth = new LinkedHashMap<>();
        for (Object[] row : lotRepo.getMonthlySpendRaw(TIMELINE_MONTHS)) {
            spendByMonth.put((String) row[0], ((Number) row[1]).doubleValue());
        }

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
                .inventoryCost(invTotals.getCost())
                .inventoryMarket(invTotals.getMarket())
                .totals7(saleRepo.getTotalsSince(LocalDate.now().minusDays(7)))
                .totals30(saleRepo.getTotalsSince(LocalDate.now().minusDays(30)))
                .monthLabels(monthLabels)
                .monthlySpend(fillSeries(monthLabels, spendByMonth))
                .monthlyGross(fillRevenueSeries(monthLabels, revenueByMonth, MonthlyRevenue::getGross))
                .monthlyNet(fillRevenueSeries(monthLabels, revenueByMonth, MonthlyRevenue::getNet))
                .originCounts(saleRepo.countByOrigin())
                .itemTypeCounts(itemRepo.countByItemType())
                .gradingStatuses(gradingRepo.countByStatus())
                .lotStatuses(lotRepo.countByStatus())
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

    private List<Double> fillRevenueSeries(List<String> labels, Map<String, MonthlyRevenue> data,
                                           ToDoubleFunction<MonthlyRevenue> extractor) {
        return labels.stream()
                .map(label -> {
                    MonthlyRevenue revenue = data.get(label);
                    return revenue != null ? extractor.applyAsDouble(revenue) : 0.0;
                })
                .toList();
    }
}
