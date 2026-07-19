package com.zakpruitt.collectingwithzak.controller;

import com.zakpruitt.collectingwithzak.dto.request.CreateInventoryRequest;
import com.zakpruitt.collectingwithzak.dto.request.InventoryItemRow;
import com.zakpruitt.collectingwithzak.dto.request.UpdateInventoryRequest;
import com.zakpruitt.collectingwithzak.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Controller
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/new")
    public String renderNewForm() {
        return "inventory/new";
    }

    @GetMapping("/partials/row")
    public String rowPartial(InventoryItemRow preset, Model model) {
        model.addAttribute("preset", preset);
        return "inventory/partials/row :: inventory-row";
    }

    @GetMapping
    public String renderIndex(@RequestParam(defaultValue = "INVENTORY") String purpose,
                              @RequestHeader(value = "HX-Request", required = false) String hx,
                              Model model) {
        if (!Set.of("INVENTORY", "IN_GRADING", "PERSONAL_COLLECTION").contains(purpose)) {
            purpose = "INVENTORY";
        }
        model.addAttribute("data", inventoryService.getIndexData(purpose));
        if (hx != null) {
            return "inventory/index :: inventory-page";
        }
        return "inventory/index";
    }

    @GetMapping("/{id}/edit")
    public String renderEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("item", inventoryService.getById(id));
        return "inventory/edit";
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<String> create(@RequestBody @Valid CreateInventoryRequest request) {
        inventoryService.createItems(request);
        return ResponseEntity.ok("/inventory?purpose=" + request.getPurpose());
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, UpdateInventoryRequest request) {
        String purpose = inventoryService.update(id, request);
        return "redirect:/inventory?purpose=" + purpose;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        String purpose = inventoryService.delete(id);
        return "redirect:/inventory?purpose=" + purpose;
    }
}
