package com.zakpruitt.collectingwithzak.service.render;

import com.zakpruitt.collectingwithzak.dto.response.LotResponse;
import com.zakpruitt.collectingwithzak.entity.LotPurchase;
import com.zakpruitt.collectingwithzak.exception.ResourceNotFoundException;
import com.zakpruitt.collectingwithzak.mapper.LotMapper;
import com.zakpruitt.collectingwithzak.repository.LotPurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LotRenderService {

    private final LotPurchaseRepository lotRepo;
    private final LotMapper lotMapper;

    public LotResponse getById(Long id) {
        LotPurchase lot = findById(id);
        return lotMapper.toResponse(lot);
    }

    public List<LotResponse> getAll() {
        List<LotPurchase> lots = lotRepo.findAllWithItemsOrderByPurchaseDateDesc();
        return lotMapper.toResponseList(lots);
    }

    private LotPurchase findById(Long id) {
        return lotRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lot", id));
    }
}
