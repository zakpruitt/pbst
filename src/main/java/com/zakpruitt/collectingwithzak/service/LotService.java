package com.zakpruitt.collectingwithzak.service;

import com.zakpruitt.collectingwithzak.dto.request.LotRequest;
import com.zakpruitt.collectingwithzak.dto.request.SnapshotItem;
import com.zakpruitt.collectingwithzak.entity.LotPurchase;
import com.zakpruitt.collectingwithzak.entity.TrackedItem;
import com.zakpruitt.collectingwithzak.entity.enums.ItemType;
import com.zakpruitt.collectingwithzak.entity.enums.LotAction;
import com.zakpruitt.collectingwithzak.entity.enums.LotStatus;
import com.zakpruitt.collectingwithzak.mapper.GradedDetailsMapper;
import com.zakpruitt.collectingwithzak.mapper.LotMapper;
import com.zakpruitt.collectingwithzak.mapper.TrackedItemMapper;
import com.zakpruitt.collectingwithzak.repository.LotPurchaseRepository;
import com.zakpruitt.collectingwithzak.repository.PokemonCardRepository;
import com.zakpruitt.collectingwithzak.repository.TrackedItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.zakpruitt.collectingwithzak.exception.ResourceNotFoundException.notFound;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class LotService {

    private final LotPurchaseRepository lotRepo;
    private final TrackedItemRepository itemRepo;
    private final PokemonCardRepository cardRepo;
    private final LotMapper lotMapper;
    private final TrackedItemMapper trackedItemMapper;
    private final GradedDetailsMapper gradedDetailsMapper;

    @Transactional(readOnly = true)
    public List<LotPurchase> getAll() {
        return lotRepo.findAllByOrderByPurchaseDateDesc();
    }

    @Transactional(readOnly = true)
    public LotPurchase getByIdWithItems(Long id) {
        return lotRepo.findWithItemsById(id).orElseThrow(notFound("Lot", id));
    }

    @Transactional(readOnly = true)
    public LotPurchase getById(Long id) {
        return lotRepo.findById(id).orElseThrow(notFound("Lot", id));
    }

    public Long create(LotRequest request) {
        LotPurchase lot = lotMapper.toEntity(request);

        lot.updateSnapshot(request.getItems());
        lotRepo.save(lot);
        log.info("Lot {} created: {} snapshot items, ${}", lot.getId(), request.getItems().size(), lot.getTotalCost());

        return lot.getId();
    }

    public void update(Long id, LotRequest request) {
        LotPurchase lot = getById(id);
        lotMapper.updateEntity(request, lot);
        lot.updateSnapshot(request.getItems());
    }

    public void updateStatus(Long id, LotAction action) {
        LotPurchase lot = getById(id);
        switch (action) {
            case ACCEPT -> accept(lot);
            case REJECT -> {
                lot.setStatus(LotStatus.REJECTED);
                log.info("Lot {} rejected", id);
            }
        }
    }

    public void delete(Long id) {
        itemRepo.deleteByLotPurchaseId(id);
        lotRepo.deleteById(id);
        log.info("Lot {} deleted with its tracked items", id);
    }

    private void accept(LotPurchase lot) {
        List<SnapshotItem> snapshot = lot.parseSnapshot();
        int created = 0;

        for (SnapshotItem item : snapshot) {
            if (!item.isTracked()) continue;

            TrackedItem trackedItem = trackedItemMapper.fromSnapshotItem(item, lot);
            if (StringUtils.hasText(item.getPokemonCardId())) {
                cardRepo.findById(item.getPokemonCardId())
                        .ifPresent(trackedItem::setPokemonCard);
            }
            if (item.isType(ItemType.GRADED_CARD)) {
                trackedItem.setGradedDetails(gradedDetailsMapper.fromSnapshotItem(item));
            }

            itemRepo.save(trackedItem);
            created++;
        }

        lot.setStatus(LotStatus.ACCEPTED);
        log.info("Lot {} accepted: {} of {} snapshot items tracked", lot.getId(), created, snapshot.size());
    }
}
