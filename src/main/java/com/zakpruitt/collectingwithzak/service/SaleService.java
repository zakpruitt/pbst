package com.zakpruitt.collectingwithzak.service;

import com.zakpruitt.collectingwithzak.dto.common.MonthGroup;
import com.zakpruitt.collectingwithzak.dto.common.VinceLedger;
import com.zakpruitt.collectingwithzak.dto.render.SaleIndexData;
import com.zakpruitt.collectingwithzak.dto.request.CreateSaleRequest;
import com.zakpruitt.collectingwithzak.dto.request.CreateVincePaymentRequest;
import com.zakpruitt.collectingwithzak.entity.Sale;
import com.zakpruitt.collectingwithzak.entity.TrackedItem;
import com.zakpruitt.collectingwithzak.entity.VincePayment;
import com.zakpruitt.collectingwithzak.entity.enums.SaleAction;
import com.zakpruitt.collectingwithzak.entity.enums.SaleStatus;
import com.zakpruitt.collectingwithzak.mapper.SaleMapper;
import com.zakpruitt.collectingwithzak.mapper.VincePaymentMapper;
import com.zakpruitt.collectingwithzak.repository.SaleRepository;
import com.zakpruitt.collectingwithzak.repository.TrackedItemRepository;
import com.zakpruitt.collectingwithzak.repository.VincePaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.zakpruitt.collectingwithzak.exception.ResourceNotFoundException.notFound;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SaleService {

    private final SaleRepository saleRepo;
    private final TrackedItemRepository itemRepo;
    private final VincePaymentRepository paymentRepo;
    private final SaleMapper saleMapper;
    private final VincePaymentMapper vincePaymentMapper;

    @Transactional(readOnly = true)
    public SaleIndexData getIndexData(String view) {
        List<MonthGroup<Sale>> groups = MonthGroup.groupByMonth(getAll(view),
                Sale::getSaleDate, Sale::getNetAmount);
        long stagedCount = saleRepo.countByStatus(SaleStatus.STAGED);

        if (!"vince".equals(view)) {
            return new SaleIndexData(groups, stagedCount, view, null, null);
        }

        VinceLedger ledger = VinceLedger.from(saleRepo.getVinceTotals(), paymentRepo.getTotals());
        List<MonthGroup<VincePayment>> paymentGroups = MonthGroup.groupByMonth(
                paymentRepo.findAllByOrderByPaymentDateDescIdDesc(),
                VincePayment::getPaymentDate, VincePayment::getAmount);
        return new SaleIndexData(groups, stagedCount, view, ledger, paymentGroups);
    }

    @Transactional(readOnly = true)
    public List<Sale> getStaged() {
        return saleRepo.findByStatusOrderBySaleDateDesc(SaleStatus.STAGED);
    }

    @Transactional(readOnly = true)
    public Sale getByIdWithItems(Long saleId) {
        return saleRepo.findWithItemsById(saleId).orElseThrow(notFound("Sale", saleId));
    }

    @Transactional(readOnly = true)
    public List<TrackedItem> getAvailableItemsFor(Sale sale) {
        return itemRepo.findAvailablePlus(sale.getItems());
    }

    public void create(CreateSaleRequest request) {
        saleRepo.save(saleMapper.toEntity(request));
    }

    public void createVincePayment(CreateVincePaymentRequest request) {
        VincePayment payment = vincePaymentMapper.toEntity(request);
        paymentRepo.save(payment);
        log.info("Vince payment recorded: {} ${}", request.getType(), request.getAmount());
    }

    public void confirmWithItems(Long saleId, List<Long> itemIds) {
        Sale sale = getByIdWithItems(saleId);
        sale.getItems().forEach(TrackedItem::releaseFromSale);
        itemRepo.findAllById(itemIds).forEach(item -> item.attachTo(sale));
        sale.setStatus(SaleStatus.CONFIRMED);
        sale.setAttributedTo("");
        log.info("Sale {} confirmed with {} items", saleId, itemIds.size());
    }

    public void updateStatus(Long saleId, SaleAction action) {
        Sale sale = getByIdWithItems(saleId);
        sale.getItems().forEach(TrackedItem::releaseFromSale);
        sale.setStatus(action.getTargetStatus());
        sale.setAttributedTo(action.getAttributedTo());
        log.info("Sale {} triaged: {}", saleId, action);
    }

    public void updateAmounts(Long saleId, double grossAmount, double netAmount) {
        Sale sale = saleRepo.findById(saleId).orElseThrow(notFound("Sale", saleId));
        sale.setGrossAmount(grossAmount);
        sale.setNetAmount(netAmount);
        log.info("Sale {} amounts set: gross=${} net=${}", saleId, grossAmount, netAmount);
    }

    public void delete(Long saleId) {
        Sale sale = getByIdWithItems(saleId);
        sale.getItems().forEach(TrackedItem::releaseFromSale);
        saleRepo.delete(sale);
        log.info("Sale {} deleted", saleId);
    }

    public void deleteVincePayment(Long id) {
        paymentRepo.deleteById(id);
        log.info("Vince payment {} deleted", id);
    }

    private List<Sale> getAll(String view) {
        return switch (view) {
            case "vince" -> saleRepo.findByStatusAndAttributedToOrderBySaleDateDesc(SaleStatus.IGNORED, "vince");
            case "ignored" -> saleRepo.findByStatusAndAttributedToOrderBySaleDateDesc(SaleStatus.IGNORED, "");
            default -> saleRepo.findByStatusOrderBySaleDateDesc(SaleStatus.CONFIRMED);
        };
    }
}
