package com.collectingwithzak.service;

import com.collectingwithzak.dto.SnapshotItem;
import com.collectingwithzak.dto.request.CreateLotRequest;
import com.collectingwithzak.dto.request.UpdateLotRequest;
import com.collectingwithzak.entity.GradedDetails;
import com.collectingwithzak.entity.LotPurchase;
import com.collectingwithzak.entity.TrackedItem;
import com.collectingwithzak.exception.ResourceNotFoundException;
import com.collectingwithzak.mapper.LotMapper;
import com.collectingwithzak.repository.LotPurchaseRepository;
import com.collectingwithzak.repository.TrackedItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LotService {

    private final LotPurchaseRepository lotRepo;
    private final TrackedItemRepository itemRepo;
    private final LotMapper lotMapper;

    public LotPurchase create(CreateLotRequest request) {
        LotPurchase lot = lotMapper.toEntity(request);
        return lotRepo.save(lot);
    }

    @Transactional(readOnly = true)
    public LotPurchase getById(Long id) {
        return lotRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lot", id));
    }

    @Transactional(readOnly = true)
    public LotPurchase getWithItems(Long id) {
        LotPurchase lot = getById(id);
        lot.getTrackedItems().size();
        return lot;
    }

    @Transactional(readOnly = true)
    public List<LotPurchase> getAll() {
        return lotRepo.findAllByOrderByPurchaseDateDesc();
    }

    public LotPurchase update(Long id, UpdateLotRequest request) {
        LotPurchase lot = getById(id);
        lotMapper.updateEntity(request, lot);
        return lotRepo.save(lot);
    }

    public void accept(Long id) {
        LotPurchase lot = getById(id);
        List<SnapshotItem> snapshot = lot.parseSnapshot();

        for (SnapshotItem item : snapshot) {
            if (!item.isTracked()) continue;
            itemRepo.save(snapshotToTrackedItem(lot, item));
        }

        lotRepo.updateStatus(id, "ACCEPTED");
    }

    public void reject(Long id) {
        lotRepo.updateStatus(id, "REJECTED");
    }

    public void delete(Long id) {
        itemRepo.deleteByLotPurchaseId(id);
        lotRepo.deleteById(id);
    }

    private TrackedItem snapshotToTrackedItem(LotPurchase lot, SnapshotItem item) {
        int qty = item.getQty() <= 0 ? 1 : item.getQty();
        String purpose = item.getPurpose();
        if ((purpose == null || purpose.isBlank()) && item.isTracked()) {
            purpose = "INVENTORY";
        }
        String itemType = item.getItemType();
        if (itemType == null || itemType.isBlank()) {
            itemType = "RAW_CARD";
        }

        TrackedItem ti = new TrackedItem();
        ti.setLotPurchase(lot);
        ti.setAcquisitionDate(lot.getPurchaseDate());
        ti.setCostBasis(item.getOffered() / qty);
        ti.setMarketValueAtPurchase(item.getMarketPrice());
        ti.setPurpose(purpose);
        ti.setItemType(itemType);
        ti.setManualNameOverride(item.getName());

        if ("GRADED_CARD".equals(item.getItemType()) && item.getGradingCompany() != null && !item.getGradingCompany().isBlank()) {
            ti.setGradedDetails(new GradedDetails(item.getGradingCompany(), item.getGrade(), 0));
        }

        return ti;
    }
}
