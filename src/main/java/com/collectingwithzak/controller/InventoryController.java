package com.collectingwithzak.controller;

import com.collectingwithzak.dto.inventory.InventoryItemRow;
import com.collectingwithzak.dto.inventory.CreateInventoryRequest;
import com.collectingwithzak.dto.inventory.UpdateInventoryRequest;
import com.collectingwithzak.dto.inventory.TrackedItemResponse;
import com.collectingwithzak.service.InventoryService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public String index(@RequestParam(defaultValue = "INVENTORY") String purpose,
                        @RequestHeader(value = "HX-Request", required = false) String hx,
                        Model model) {
        model.addAttribute("data", inventoryService.getIndexData(purpose));
        if (hx != null) {
            return "inventory/index :: inventory-page";
        }
        return "inventory/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        return "inventory/new";
    }

    @GetMapping("/partials/row")
    public String rowPartial(InventoryItemRow preset, Model model) {
        model.addAttribute("preset", preset);
        return "inventory/partials/row :: inventory-row";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        TrackedItemResponse item = inventoryService.getById(id);
        model.addAttribute("item", item);
        return "inventory/edit";
    }

    @PostMapping
    public String create(@Valid CreateInventoryRequest request) {
        inventoryService.createItems(request);
        return "redirect:/inventory?purpose=" + request.getPurpose();
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
