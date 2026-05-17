package com.collectingwithzak.service;

import com.collectingwithzak.dto.SnapshotItem;
import com.collectingwithzak.dto.request.CreateLotRequest;
import com.collectingwithzak.dto.request.UpdateLotRequest;
import com.collectingwithzak.dto.response.LotResponse;
import com.collectingwithzak.entity.GradedDetails;
import com.collectingwithzak.entity.LotPurchase;
import com.collectingwithzak.entity.TrackedItem;
import com.collectingwithzak.entity.enums.ItemType;
import com.collectingwithzak.entity.enums.LotStatus;
import com.collectingwithzak.entity.enums.Purpose;
import org.springframework.util.StringUtils;
import com.collectingwithzak.exception.ResourceNotFoundException;
import com.collectingwithzak.mapper.LotMapper;
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

    // ---------- Create ----------

    public Long create(CreateLotRequest request) {
        LotPurchase lot = lotMapper.toEntity(request);
        return lotRepo.save(lot).getId();
    }

    // ---------- Read ----------

    public LotResponse getById(Long id) {
        LotPurchase lot = findById(id);
        return lotMapper.toResponse(lot);
    }


    public List<LotResponse> getAll() {
        return lotMapper.toResponseList(lotRepo.findAllWithItemsOrderByPurchaseDateDesc());
    }

    // ---------- Update ----------

    public void update(Long id, UpdateLotRequest request) {
        LotPurchase lot = findById(id);
        lotMapper.updateEntity(request, lot);
        lotRepo.save(lot);
    }

    public void accept(Long id) {
        LotPurchase lot = findById(id);
        List<SnapshotItem> snapshot = lot.parseSnapshot();

        for (SnapshotItem item : snapshot) {
            if (!item.isTracked()) continue;
            itemRepo.save(snapshotToTrackedItem(lot, item));
        }

        lotRepo.updateStatus(id, LotStatus.ACCEPTED.name());
    }

    public void reject(Long id) {
        lotRepo.updateStatus(id, LotStatus.REJECTED.name());
    }

    // ---------- Delete ----------

    public void delete(Long id) {
        itemRepo.deleteByLotPurchaseId(id);
        lotRepo.deleteById(id);
    }

    // ---------- Helpers ----------

    private LotPurchase findById(Long id) {
        return lotRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lot", id));
    }

    private TrackedItem snapshotToTrackedItem(LotPurchase lot, SnapshotItem item) {
        int qty = item.getQty() <= 0 ? 1 : item.getQty();
        String purpose = StringUtils.hasText(item.getPurpose()) ? item.getPurpose() : Purpose.INVENTORY.name();
        String itemType = StringUtils.hasText(item.getItemType()) ? item.getItemType() : ItemType.RAW_CARD.name();

        var builder = TrackedItem.builder()
                .lotPurchase(lot)
                .acquisitionDate(lot.getPurchaseDate())
                .costBasis(item.getOffered() / qty)
                .marketValueAtPurchase(item.getMarketPrice())
                .purpose(purpose)
                .itemType(itemType)
                .manualNameOverride(item.getName());

        if (StringUtils.hasText(item.getPokemonCardId())) {
            builder.pokemonCard(cardRepo.findById(item.getPokemonCardId()).orElse(null));
        }

        if (ItemType.GRADED_CARD.name().equals(item.getItemType())
                && StringUtils.hasText(item.getGradingCompany())) {
            builder.gradedDetails(GradedDetails.builder()
                    .gradingCompany(item.getGradingCompany())
                    .grade(item.getGrade())
                    .build());
        }

        return builder.build();
    }
}
