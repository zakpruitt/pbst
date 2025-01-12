package com.zakpruitt.pbst.services;

import com.zakpruitt.pbst.dtos.TrackedItemDTO;
import com.zakpruitt.pbst.entities.GradedDetails;
import com.zakpruitt.pbst.entities.LotPurchase;
import com.zakpruitt.pbst.entities.TrackedItem;
import com.zakpruitt.pbst.enums.ItemGradingStatus;
import com.zakpruitt.pbst.enums.ItemType;
import com.zakpruitt.pbst.enums.Purpose;
import com.zakpruitt.pbst.exception.ResourceNotFoundException;
import com.zakpruitt.pbst.mappers.TrackedItemMapper;
import com.zakpruitt.pbst.repositories.LotPurchaseRepository;
import com.zakpruitt.pbst.repositories.TrackedItemRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@AllArgsConstructor
public class TrackedItemService {

    private final TrackedItemRepository trackedItemRepository;
    private final TrackedItemMapper trackedItemMapper;
    private final LotPurchaseRepository lotPurchaseRepository;

    public List<TrackedItemDTO> getAllTrackedItems() {
        return trackedItemRepository.findAllAccepted().stream()
                .map(trackedItemMapper::toDto)
                .collect(Collectors.toList());
    }

    public TrackedItemDTO getTrackedItemById(Long id) {
        return trackedItemRepository.findById(id)
                .map(trackedItemMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("TrackedItem not found with id: " + id));
    }

    public TrackedItemDTO saveTrackedItem(TrackedItemDTO dto) {
        LotPurchase lot = null;
        if (dto.getLotPurchaseId() != null) {
            lot = lotPurchaseRepository.findById(dto.getLotPurchaseId())
                    .orElseThrow(() -> new ResourceNotFoundException("LotPurchase not found with id: " + dto.getLotPurchaseId()));
        }

        TrackedItem item = trackedItemMapper.toEntity(dto);
        item.setLotPurchase(lot); // Ensure lot is set

        if (dto.getItemType() == ItemType.GRADED_CARD) {
            item.setGradingStatus(ItemGradingStatus.GRADED);
            if (item.getGradedDetails() == null) {
                item.setGradedDetails(new GradedDetails());
            }
            item.getGradedDetails().setGradingCompany(dto.getGradingCompany());
            item.getGradedDetails().setGrade(dto.getGrade());
            item.getGradedDetails().setGradingUpcharge(dto.getGradingUpcharge());
        }

        TrackedItem savedItem = trackedItemRepository.save(item);
        return trackedItemMapper.toDto(savedItem);
    }
    
    public TrackedItemDTO updateTrackedItem(TrackedItemDTO dto) {
        TrackedItem existingItem = trackedItemRepository.findById(dto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("TrackedItem not found with id: " + dto.getId()));
        
        // Update allowed fields
        existingItem.setManualNameOverride(dto.getName()); // MapStruct maps name -> manualNameOverride
        existingItem.setCostBasis(dto.getCostBasis());
        
        // Only allow purpose change if not in grading
        if (existingItem.getGradingStatus() != ItemGradingStatus.IN_GRADING) {
            existingItem.setPurpose(dto.getPurpose());
        }
        
        TrackedItem savedItem = trackedItemRepository.save(existingItem);
        return trackedItemMapper.toDto(savedItem);
    }

    public void deleteTrackedItem(Long id) {
        if (!trackedItemRepository.existsById(id)) {
            throw new ResourceNotFoundException("TrackedItem not found with id: " + id);
        }
        trackedItemRepository.deleteById(id);
    }

    public List<TrackedItem> getItemsByPurpose(Purpose purpose) {
        return trackedItemRepository.findByPurposeAccepted(purpose);
    }

    public long countByPurpose(Purpose purpose) {
        return trackedItemRepository.countByPurposeAccepted(purpose);
    }

    public List<TrackedItemDTO> getItemsByGradingStatus(ItemGradingStatus status) {
        if (status == ItemGradingStatus.TO_GRADE) {
            return trackedItemRepository.findItemsToGradeAccepted(status).stream()
                    .map(trackedItemMapper::toDto)
                    .collect(Collectors.toList());
        }
        return trackedItemRepository.findByGradingStatus(status).stream()
                .filter(item -> "ACCEPTED".equals(item.getLotPurchase().getStatus()))
                .map(trackedItemMapper::toDto)
                .collect(Collectors.toList());
    }

    public BigDecimal getUnsoldMarketValue() {
        BigDecimal value = trackedItemRepository.sumUnsoldMarketValue();
        return value != null ? value : BigDecimal.ZERO;
    }

    public BigDecimal getCostBasisByPurpose(Purpose purpose) {
        BigDecimal value = trackedItemRepository.sumCostBasisByPurpose(purpose);
        return value != null ? value : BigDecimal.ZERO;
    }

    public BigDecimal getMarketValueByGradingStatus(ItemGradingStatus status) {
        BigDecimal value = trackedItemRepository.sumMarketValueByGradingStatus(status);
        return value != null ? value : BigDecimal.ZERO;
    }
    
    public BigDecimal getUnsoldMarketValueByPurpose(Purpose purpose) {
        BigDecimal value = trackedItemRepository.sumUnsoldMarketValueByPurpose(purpose);
        return value != null ? value : BigDecimal.ZERO;
    }
}
