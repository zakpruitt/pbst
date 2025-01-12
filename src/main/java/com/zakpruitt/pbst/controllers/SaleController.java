package com.zakpruitt.pbst.controllers;

import com.zakpruitt.pbst.dtos.SaleDTO;
import com.zakpruitt.pbst.services.SaleService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
@AllArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @GetMapping
    public ResponseEntity<List<SaleDTO>> getAllSales() {
        return ResponseEntity.ok(saleService.getAllSales());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaleDTO> getSaleById(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.getSaleById(id));
    }

    @PostMapping
    public ResponseEntity<SaleDTO> createSale(@RequestBody SaleDTO saleDTO) {
        return ResponseEntity.ok(saleService.saveSale(saleDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSale(@PathVariable Long id) {
        saleService.deleteSale(id);
        return ResponseEntity.noContent().build();
    }
}
