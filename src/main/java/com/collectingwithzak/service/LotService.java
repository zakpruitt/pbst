package com.collectingwithzak.service;

import com.collectingwithzak.dto.request.LotRequest;
import com.collectingwithzak.dto.request.SnapshotItem;
import com.collectingwithzak.entity.LotPurchase;
import com.collectingwithzak.entity.TrackedItem;
import com.collectingwithzak.entity.enums.LotAction;
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

    public void update(Long id, LotRequest request) {
        LotPurchase lot = findById(id);
        lotMapper.updateEntity(request, lot);
    }

    public void updateStatus(Long id, LotAction action) {
        LotPurchase lot = findById(id);
        switch (action) {
            case ACCEPT -> accept(lot);
            case REJECT -> lot.setStatus(LotStatus.REJECTED);
        }
    }

    public void delete(Long id) {
        itemRepo.deleteByLotPurchaseId(id);
        lotRepo.deleteById(id);
    }

    private void accept(LotPurchase lot) {
        List<SnapshotItem> snapshot = lot.parseSnapshot();

        for (SnapshotItem item : snapshot) {
            if (!item.isTracked()) continue;

            TrackedItem trackedItem = trackedItemMapper.fromSnapshotItem(item, lot);

            if (!item.getPokemonCardId().isEmpty()) {
                trackedItem.setPokemonCard(cardRepo.findById(item.getPokemonCardId()).orElse(null));
            }

            if ("GRADED_CARD".equals(item.getItemType()) && !item.getGradingCompany().isEmpty()) {
                trackedItem.setGradedDetails(gradedDetailsMapper.fromSnapshotItem(item));
            }

            itemRepo.save(trackedItem);
        }

        lot.setStatus(LotStatus.ACCEPTED);
    }

    private LotPurchase findById(Long id) {
        return lotRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lot", id));
    }
}
