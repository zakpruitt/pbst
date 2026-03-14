package com.zakpruitt.pbst.controllers;

import com.zakpruitt.pbst.dtos.LotPurchaseDTO;
import com.zakpruitt.pbst.services.LotPurchaseService;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/lots")
@AllArgsConstructor
public class LotPurchaseController {

    private final LotPurchaseService lotPurchaseService;

    @GetMapping
    public ResponseEntity<List<LotPurchaseDTO>> getAllLots() {
        return ResponseEntity.ok(lotPurchaseService.getAllLots());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LotPurchaseDTO> getLotById(@PathVariable Long id) {
        return ResponseEntity.ok(lotPurchaseService.getLotById(id));
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> createLot(@ModelAttribute LotPurchaseDTO lotPurchaseDTO) {
        // Default to PENDING if not set
        if (lotPurchaseDTO.getStatus() == null) {
            lotPurchaseDTO.setStatus("PENDING");
        }
        
        // Filter out null/empty tracked items that might come from the form index gaps
        if (lotPurchaseDTO.getTrackedItems() != null) {
            lotPurchaseDTO.setTrackedItems(
                lotPurchaseDTO.getTrackedItems().stream()
                    .filter(item -> item.getIsTracked() != null && item.getIsTracked()) // Only save items marked as tracked
                    .toList()
            );
        }

        LotPurchaseDTO savedLot = lotPurchaseService.saveLot(lotPurchaseDTO);
        
        // Return a redirect to the UI view, not the JSON
        return ResponseEntity.status(303).location(URI.create("/lots/" + savedLot.getId())).build();
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> updateLot(@PathVariable Long id, @ModelAttribute LotPurchaseDTO lotPurchaseDTO) {
        LotPurchaseDTO updatedLot = lotPurchaseService.updateLotDetails(id, lotPurchaseDTO);
        return ResponseEntity.status(303).location(URI.create("/lots/" + updatedLot.getId())).build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateLotStatus(@PathVariable Long id, @RequestParam String status) {
        lotPurchaseService.updateStatus(id, status);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLot(@PathVariable Long id) {
        lotPurchaseService.deleteLot(id);
        return ResponseEntity.noContent().build();
    }
}
