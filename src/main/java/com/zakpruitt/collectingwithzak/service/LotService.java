package com.zakpruitt.collectingwithzak.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zakpruitt.collectingwithzak.dto.request.LotRequest;
import com.zakpruitt.collectingwithzak.dto.request.SnapshotItem;
import com.zakpruitt.collectingwithzak.entity.LotPurchase;
import com.zakpruitt.collectingwithzak.entity.TrackedItem;
import com.zakpruitt.collectingwithzak.entity.enums.LotAction;
import com.zakpruitt.collectingwithzak.entity.enums.LotStatus;
import com.zakpruitt.collectingwithzak.mapper.GradedDetailsMapper;
import com.zakpruitt.collectingwithzak.mapper.LotMapper;
import com.zakpruitt.collectingwithzak.mapper.TrackedItemMapper;
import com.zakpruitt.collectingwithzak.repository.LotPurchaseRepository;
import com.zakpruitt.collectingwithzak.repository.PokemonCardRepository;
import com.zakpruitt.collectingwithzak.repository.TrackedItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.zakpruitt.collectingwithzak.exception.ResourceNotFoundException.notFound;

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
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<LotPurchase> getAll() {
        return lotRepo.findAllWithItemsOrderByPurchaseDateDesc();
    }

    @Transactional(readOnly = true)
    public LotPurchase getByIdWithItems(Long id) {
        return lotRepo.findWithItemsById(id).orElseThrow(notFound("Lot", id));
    }

    /**
     * Lot fields and snapshot only — enough for the edit form, which renders no tracked items.
     */
    @Transactional(readOnly = true)
    public LotPurchase getById(Long id) {
        return lotRepo.findById(id).orElseThrow(notFound("Lot", id));
    }

    public Long create(LotRequest request) {
        LotPurchase lot = lotMapper.toEntity(request);
        lot.setLotContentSnapshot(serializeItems(request.getItems()));
        return lotRepo.save(lot).getId();
    }

    public void update(Long id, LotRequest request) {
        LotPurchase lot = getById(id);
        lotMapper.updateEntity(request, lot);
        lot.setLotContentSnapshot(serializeItems(request.getItems()));
    }

    public void updateStatus(Long id, LotAction action) {
        LotPurchase lot = getById(id);
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

            if (item.getPokemonCardId() != null && !item.getPokemonCardId().isEmpty()) {
                cardRepo.findById(item.getPokemonCardId())
                        .ifPresent(trackedItem::setPokemonCard);
            }

            if ("GRADED_CARD".equals(item.getItemType())) {
                trackedItem.setGradedDetails(gradedDetailsMapper.fromSnapshotItem(item));
            }

            itemRepo.save(trackedItem);
        }

        lot.setStatus(LotStatus.ACCEPTED);
    }

    private String serializeItems(List<SnapshotItem> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize snapshot items", e);
        }
    }
}
