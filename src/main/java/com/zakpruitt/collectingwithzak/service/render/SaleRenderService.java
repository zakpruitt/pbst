package com.zakpruitt.collectingwithzak.service.render;

import com.zakpruitt.collectingwithzak.dto.common.MonthGroup;
import com.zakpruitt.collectingwithzak.dto.common.RangeTotals;
import com.zakpruitt.collectingwithzak.dto.common.VincePaymentTotals;
import com.zakpruitt.collectingwithzak.dto.render.SaleIndexData;
import com.zakpruitt.collectingwithzak.dto.response.SaleResponse;
import com.zakpruitt.collectingwithzak.dto.response.TrackedItemResponse;
import com.zakpruitt.collectingwithzak.dto.response.VinceLedger;
import com.zakpruitt.collectingwithzak.dto.response.VincePaymentResponse;
import com.zakpruitt.collectingwithzak.entity.Sale;
import com.zakpruitt.collectingwithzak.entity.TrackedItem;
import com.zakpruitt.collectingwithzak.entity.enums.ItemStatus;
import com.zakpruitt.collectingwithzak.entity.enums.SaleStatus;
import com.zakpruitt.collectingwithzak.exception.ResourceNotFoundException;
import com.zakpruitt.collectingwithzak.mapper.SaleMapper;
import com.zakpruitt.collectingwithzak.mapper.TrackedItemMapper;
import com.zakpruitt.collectingwithzak.mapper.VincePaymentMapper;
import com.zakpruitt.collectingwithzak.repository.SaleRepository;
import com.zakpruitt.collectingwithzak.repository.TrackedItemRepository;
import com.zakpruitt.collectingwithzak.repository.VincePaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SaleRenderService {

    private final SaleRepository saleRepo;
    private final TrackedItemRepository itemRepo;
    private final VincePaymentRepository paymentRepo;
    private final SaleMapper saleMapper;
    private final TrackedItemMapper trackedItemMapper;
    private final VincePaymentMapper paymentMapper;

    public SaleIndexData getIndexData(String view) {
        List<SaleResponse> sales = getAll(view);
        List<MonthGroup<SaleResponse>> groups = MonthGroup.groupByMonth(sales, SaleResponse::getSaleDate, SaleResponse::getNetAmount);

        SaleIndexData.SaleIndexDataBuilder builder = SaleIndexData.builder()
                .groups(groups)
                .stagedCount(countStaged())
                .view(view);

        if ("vince".equals(view)) {
            RangeTotals vinceSalesTotals = saleRepo.getVinceTotals();
            VincePaymentTotals paymentTotals = paymentRepo.getTotals();
            builder.vinceLedger(VinceLedger.from(vinceSalesTotals, paymentTotals.getPaidOut(), paymentTotals.getVinceOwes()));

            List<VincePaymentResponse> payments = paymentMapper.toResponseList(paymentRepo.findAllByOrderByPaymentDateDescIdDesc());
            List<MonthGroup<VincePaymentResponse>> paymentGroups = MonthGroup.groupByMonth(payments, VincePaymentResponse::getPaymentDate, VincePaymentResponse::getAmount);
            builder.vincePaymentGroups(paymentGroups);
        }

        return builder.build();
    }

    public SaleResponse getByIdWithItems(Long id) {
        Sale sale = findByIdWithItems(id);
        return saleMapper.toResponse(sale);
    }

    public List<SaleResponse> getAll(String view) {
        List<Sale> sales = switch (view) {
            case "vince" -> saleRepo.findByStatusAndAttributedToOrderBySaleDateDesc(SaleStatus.IGNORED, "vince");
            case "ignored" -> saleRepo.findIgnored();
            default -> saleRepo.findByStatusOrderBySaleDateDesc(SaleStatus.CONFIRMED);
        };
        return saleMapper.toResponseList(sales);
    }

    public List<SaleResponse> getStaged() {
        return saleMapper.toResponseList(saleRepo.findByStatusOrderBySaleDateDesc(SaleStatus.STAGED));
    }

    public long countStaged() {
        return saleRepo.countByStatus(SaleStatus.STAGED);
    }

    public List<TrackedItemResponse> getAvailableItemsForSale(Long saleId) {
        Sale sale = findByIdWithItems(saleId);

        List<TrackedItem> available = itemRepo.findByStatusAndSaleIsNull(ItemStatus.AVAILABLE);
        List<TrackedItem> allItems = new ArrayList<>(available);
        for (TrackedItem attached : sale.getItems()) {
            if (!allItems.contains(attached)) {
                allItems.add(attached);
            }
        }
        return trackedItemMapper.toResponseList(allItems);
    }

    private Sale findByIdWithItems(Long id) {
        return saleRepo.findWithItemsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", id));
    }
}
