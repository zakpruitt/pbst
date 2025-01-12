package com.zakpruitt.pbst.services;

import com.zakpruitt.pbst.dtos.SaleDTO;
import com.zakpruitt.pbst.entities.Sale;
import com.zakpruitt.pbst.exception.ResourceNotFoundException;
import com.zakpruitt.pbst.mappers.SaleMapper;
import com.zakpruitt.pbst.repositories.SaleRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleMapper saleMapper;

    public List<SaleDTO> getAllSales() {
        return saleRepository.findAll().stream()
                .map(saleMapper::toDto)
                .collect(Collectors.toList());
    }

    public SaleDTO getSaleById(Long id) {
        return saleRepository.findById(id)
                .map(saleMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with id: " + id));
    }

    public SaleDTO saveSale(SaleDTO saleDTO) {
        Sale sale = saleMapper.toEntity(saleDTO);
        Sale savedSale = saleRepository.save(sale);
        return saleMapper.toDto(savedSale);
    }

    public void deleteSale(Long id) {
        if (!saleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Sale not found with id: " + id);
        }
        saleRepository.deleteById(id);
    }

    public BigDecimal getTotalNet() {
        return saleRepository.sumNetAmount();
    }

    /**
     * Placeholder for future eBay API integration.
     * This method would fetch orders from eBay and map them to Sale entities.
     */
    public void syncEbaySales() {
        log.info("Starting eBay sales sync (placeholder)...");
        // Logic to call eBay API and save/update Sale entities
    }
}
