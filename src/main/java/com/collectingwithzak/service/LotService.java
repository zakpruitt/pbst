package com.collectingwithzak.service;

import com.collectingwithzak.dto.lot.LotRequest;
import com.collectingwithzak.dto.lot.LotResponse;
import com.collectingwithzak.dto.lot.SnapshotItem;
import com.collectingwithzak.entity.LotPurchase;
import com.collectingwithzak.entity.TrackedItem;
import com.collectingwithzak.entity.enums.ItemType;
import com.collectingwithzak.entity.enums.LotStatus;
import com.collectingwithzak.exception.ResourceNotFoundException;
import com.collectingwithzak.mapper.GradedDetailsMapper;
import com.collectingwithzak.mapper.LotMapper;
import com.collectingwithzak.mapper.TrackedItemMapper;
import com.collectingwithzak.repository.LotPurchaseRepository;
import com.collectingwithzak.repository.PokemonCardRepository;
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
    private final PokemonCardRepository cardRepo;
    private final LotMapper lotMapper;
    private final TrackedItemMapper trackedItemMapper;
    private final GradedDetailsMapper gradedDetailsMapper;

    public Long create(LotRequest request) {
        LotPurchase lot = lotMapper.toEntity(request);
        return lotRepo.save(lot).getId();
    }

    public LotResponse getById(Long id) {
        LotPurchase lot = lotRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lot", id));
        return lotMapper.toResponse(lot);
    }

    public List<LotResponse> getAll() {
        List<LotPurchase> lots = lotRepo.findAllWithItemsOrderByPurchaseDateDesc();
        return lotMapper.toResponseList(lots);
    }

    public void update(Long id, LotRequest request) {
        LotPurchase lot = lotRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lot", id));
        lotMapper.updateEntity(request, lot);
        lotRepo.save(lot);
    }

    public void updateStatus(Long id, String action) {
        switch (action) {
            case "accept" -> accept(id);
            case "reject" -> lotRepo.updateStatus(id, LotStatus.REJECTED.name());
        }
    }

    public void delete(Long id) {
        itemRepo.deleteByLotPurchaseId(id);
        lotRepo.deleteById(id);
    }

    private void accept(Long id) {
        LotPurchase lot = lotRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lot", id));
        List<SnapshotItem> snapshot = lot.parseSnapshot();

        for (SnapshotItem item : snapshot) {
            if (!item.isTracked()) continue;
            itemRepo.save(snapshotToTrackedItem(lot, item));
        }

        lotRepo.updateStatus(id, LotStatus.ACCEPTED.name());
    }

    private TrackedItem snapshotToTrackedItem(LotPurchase lot, SnapshotItem item) {
        TrackedItem trackedItem = trackedItemMapper.fromSnapshotItem(item);
        trackedItem.setLotPurchase(lot);
        trackedItem.setAcquisitionDate(lot.getPurchaseDate());
        trackedItem.setCostBasis(item.getOffered() / item.getQty());

        if (!item.getPokemonCardId().isEmpty()) {
            trackedItem.setPokemonCard(cardRepo.findById(item.getPokemonCardId()).orElse(null));
        }

        if (ItemType.GRADED_CARD.name().equals(item.getItemType())
                && !item.getGradingCompany().isEmpty()) {
            trackedItem.setGradedDetails(gradedDetailsMapper.fromSnapshotItem(item));
        }

        return trackedItem;
    }
}
