package com.zakpruitt.pbst.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zakpruitt.pbst.dtos.LotPurchaseDTO;
import com.zakpruitt.pbst.dtos.TrackedItemDTO;
import com.zakpruitt.pbst.entities.GradedDetails;
import com.zakpruitt.pbst.entities.LotPurchase;
import com.zakpruitt.pbst.entities.TrackedItem;
import com.zakpruitt.pbst.enums.ItemGradingStatus;
import com.zakpruitt.pbst.enums.ItemType;
import com.zakpruitt.pbst.enums.Purpose;
import com.zakpruitt.pbst.exception.ResourceNotFoundException;
import com.zakpruitt.pbst.mappers.LotPurchaseMapper;
import com.zakpruitt.pbst.mappers.TrackedItemMapper;
import com.zakpruitt.pbst.repositories.LotPurchaseRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class LotPurchaseService {

    private final LotPurchaseRepository lotPurchaseRepository;
    private final LotPurchaseMapper lotPurchaseMapper;
    private final TrackedItemMapper trackedItemMapper;
    private final ObjectMapper objectMapper;

    public List<LotPurchaseDTO> getAllLots() {
        return lotPurchaseRepository.findAll().stream()
                .map(lotPurchaseMapper::toDto)
                .collect(Collectors.toList());
    }

    public LotPurchaseDTO getLotById(Long id) {
        return lotPurchaseRepository.findById(id)
                .map(lotPurchaseMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("LotPurchase not found with id: " + id));
    }

    public LotPurchaseDTO saveLot(LotPurchaseDTO dto) {
        LotPurchase lot = lotPurchaseMapper.toEntity(dto);
        
        calculateEstimatedMarketValue(lot);
        processTrackedItems(lot, dto.getTrackedItems());

        LotPurchase savedLot = lotPurchaseRepository.save(lot);
        return lotPurchaseMapper.toDto(savedLot);
    }

    public LotPurchaseDTO updateLotDetails(Long id, LotPurchaseDTO dto) {
        LotPurchase lot = lotPurchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LotPurchase not found with id: " + id));
        
        lot.setDescription(dto.getDescription());
        lot.setPurchaseDate(dto.getPurchaseDate());
        lot.setTotalCost(dto.getTotalCost());
        lot.setLotContentSnapshot(dto.getLotContentSnapshot());
        
        calculateEstimatedMarketValue(lot);
        
        LotPurchase savedLot = lotPurchaseRepository.save(lot);
        return lotPurchaseMapper.toDto(savedLot);
    }

    public void updateStatus(Long id, String status) {
        LotPurchase lot = lotPurchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LotPurchase not found with id: " + id));
        lot.setStatus(status);
        lotPurchaseRepository.save(lot);
    }

    public void deleteLot(Long id) {
        if (!lotPurchaseRepository.existsById(id)) {
            throw new ResourceNotFoundException("LotPurchase not found with id: " + id);
        }
        lotPurchaseRepository.deleteById(id);
    }

    public BigDecimal getTotalCost() {
        return lotPurchaseRepository.sumTotalCost();
    }

    // --- Helper Methods ---

    private void calculateEstimatedMarketValue(LotPurchase lot) {
        if (lot.getLotContentSnapshot() == null || lot.getLotContentSnapshot().isEmpty()) {
            return;
        }
        try {
            List<Map<String, Object>> snapshotItems = objectMapper.readValue(lot.getLotContentSnapshot(), new TypeReference<>() {});
            BigDecimal totalMarket = snapshotItems.stream()
                    .map(this::calculateItemMarketValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            lot.setEstimatedMarketValue(totalMarket);
        } catch (Exception e) {
            log.warn("Failed to parse lot snapshot for market value calculation: {}", e.getMessage());
        }
    }

    private BigDecimal calculateItemMarketValue(Map<String, Object> item) {
        try {
            BigDecimal market = new BigDecimal(String.valueOf(item.getOrDefault("market", "0")));
            BigDecimal qty = new BigDecimal(String.valueOf(item.getOrDefault("qty", "1")));
            return market.multiply(qty);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private void processTrackedItems(LotPurchase lot, List<TrackedItemDTO> itemDtos) {
        if (itemDtos == null || itemDtos.isEmpty()) {
            lot.setTrackedItems(Collections.emptyList());
            return;
        }

        List<TrackedItem> items = itemDtos.stream()
                .filter(dto -> dto.getIsTracked() != null && dto.getIsTracked())
                .flatMap(dto -> IntStream.range(0, dto.getQuantity() != null ? dto.getQuantity() : 1)
                        .mapToObj(i -> mapToEntityWithRelationships(dto, lot)))
                .collect(Collectors.toList());
        
        lot.setTrackedItems(items);
    }

    private TrackedItem mapToEntityWithRelationships(TrackedItemDTO dto, LotPurchase lot) {
        TrackedItem item = trackedItemMapper.toEntity(dto);
        item.setLotPurchase(lot);
        
        // Sync grading status with purpose
        if (item.getPurpose() == Purpose.TO_GRADE && (item.getGradingStatus() == null || item.getGradingStatus() == ItemGradingStatus.NONE)) {
            item.setGradingStatus(ItemGradingStatus.TO_GRADE);
        }

        // Handle pre-graded slabs
        if (dto.getItemType() == ItemType.GRADED_CARD) {
            item.setGradingStatus(ItemGradingStatus.GRADED);
            if (item.getGradedDetails() == null) {
                item.setGradedDetails(new GradedDetails());
            }
            item.getGradedDetails().setGradingCompany(dto.getGradingCompany());
            item.getGradedDetails().setGrade(dto.getGrade());
            item.getGradedDetails().setGradingUpcharge(dto.getGradingUpcharge());
        }
        
        // Prevent TransientObjectException by nulling out empty relationships
        if (dto.getGradingSubmissionId() == null) item.setGradingSubmission(null);
        if (dto.getSaleId() == null) item.setSale(null);
        
        return item;
    }
}
