package com.collectingwithzak.controller;

import com.collectingwithzak.dto.request.CreateInventoryRequest;
import com.collectingwithzak.dto.request.InventoryItemRow;
import com.collectingwithzak.dto.request.UpdateInventoryRequest;
import com.collectingwithzak.dto.response.TrackedItemResponse;
import com.collectingwithzak.service.InventoryService;
import com.collectingwithzak.service.render.InventoryRenderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryRenderService inventoryRenderService;
    private final InventoryService inventoryService;

    @GetMapping
    public String renderIndex(@RequestParam(defaultValue = "INVENTORY") String purpose,
                              @RequestHeader(value = "HX-Request", required = false) String hx,
                              Model model) {
        model.addAttribute("data", inventoryRenderService.getIndexData(purpose));
        if (hx != null) {
            return "inventory/index :: inventory-page";
        }
        return "inventory/index";
    }

    @GetMapping("/new")
    public String renderNewForm(Model model) {
        return "inventory/new";
    }

    @GetMapping("/partials/row")
    public String rowPartial(InventoryItemRow preset, Model model) {
        model.addAttribute("preset", preset);
        return "inventory/partials/row :: inventory-row";
    }

    @GetMapping("/{id}/edit")
    public String renderEditForm(@PathVariable Long id, Model model) {
        TrackedItemResponse item = inventoryRenderService.getById(id);
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
