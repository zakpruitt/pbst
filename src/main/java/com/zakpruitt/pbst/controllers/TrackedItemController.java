package com.zakpruitt.pbst.controllers;

import com.zakpruitt.pbst.dtos.TrackedItemDTO;
import com.zakpruitt.pbst.services.TrackedItemService;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tracked-items")
@AllArgsConstructor
public class TrackedItemController {

    private final TrackedItemService trackedItemService;

    @GetMapping
    public ResponseEntity<List<TrackedItemDTO>> getAllTrackedItems() {
        return ResponseEntity.ok(trackedItemService.getAllTrackedItems());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrackedItemDTO> getTrackedItemById(@PathVariable Long id) {
        return ResponseEntity.ok(trackedItemService.getTrackedItemById(id));
    }

    @PostMapping
    public ResponseEntity<TrackedItemDTO> createTrackedItem(@RequestBody TrackedItemDTO dto) {
        return ResponseEntity.ok(trackedItemService.saveTrackedItem(dto));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String updateTrackedItem(@PathVariable Long id, @ModelAttribute TrackedItemDTO dto, Model model) {
        // Ensure ID matches
        dto.setId(id);
        TrackedItemDTO updatedItem = trackedItemService.updateTrackedItem(dto);
        
        model.addAttribute("item", updatedItem);
        return "fragments/tracked-item-row :: trackedItemRow"; 
    }
    
    @GetMapping("/{id}/edit")
    public String editTrackedItem(@PathVariable Long id, Model model) {
        TrackedItemDTO item = trackedItemService.getTrackedItemById(id);
        model.addAttribute("item", item);
        return "fragments/tracked-item-edit-row :: trackedItemEditRow";
    }

    @GetMapping("/{id}/row")
    public String getTrackedItemRow(@PathVariable Long id, Model model) {
        TrackedItemDTO item = trackedItemService.getTrackedItemById(id);
        model.addAttribute("item", item);
        return "fragments/tracked-item-row :: trackedItemRow";
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrackedItem(@PathVariable Long id) {
        trackedItemService.deleteTrackedItem(id);
        return ResponseEntity.noContent().build();
    }
}
