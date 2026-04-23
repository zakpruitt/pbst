package com.collectingwithzak.controller;

import com.collectingwithzak.dto.InventoryRowPreset;
import com.collectingwithzak.dto.request.CreateInventoryRequest;
import com.collectingwithzak.dto.request.UpdateInventoryRequest;
import com.collectingwithzak.dto.response.InventorySplitResponse;
import com.collectingwithzak.dto.response.TrackedItemResponse;
import com.collectingwithzak.entity.enums.Purpose;
import com.collectingwithzak.service.InventoryService;
import jakarta.servlet.http.HttpServletRequest;
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
                        HttpServletRequest request, Model model) {
        InventorySplitResponse split = inventoryService.getByPurpose(purpose);

        model.addAttribute("page", "inventory");
        model.addAttribute("items", split.getAllItems());
        model.addAttribute("rawItems", split.getRawItems());
        model.addAttribute("gradedItems", split.getGradedItems());
        model.addAttribute("sealedItems", split.getSealedItems());
        model.addAttribute("otherItems", split.getOtherItems());
        model.addAttribute("purpose", purpose);

        if ("true".equals(request.getHeader("HX-Request"))) {
            return "inventory/index :: inventory-page";
        }
        return "inventory/index";
    }

    @GetMapping("/partials/row")
    public String rowPartial(@RequestParam(defaultValue = "OTHER") String type,
                             @RequestParam(defaultValue = "") String name,
                             @RequestParam(value = "set", defaultValue = "") String setName,
                             @RequestParam(value = "card", defaultValue = "") String cardNumber,
                             @RequestParam(defaultValue = "0") double market,
                             @RequestParam(value = "img", defaultValue = "") String imageUrl,
                             @RequestParam(value = "card_id", defaultValue = "") String pokemonCardId,
                             @RequestParam(value = "sealed_id", defaultValue = "") String sealedProductId,
                             @RequestParam(value = "grading_company", defaultValue = "") String gradingCompany,
                             Model model) {
        model.addAttribute("preset", new InventoryRowPreset(
                name, type, 0, market, pokemonCardId, sealedProductId,
                setName, cardNumber, imageUrl, gradingCompany));
        return "inventory/partials/row :: inventory-row";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("page", "inventory");
        return "inventory/new";
    }

    @PostMapping
    public String create(CreateInventoryRequest request) {
        if (request.getPurpose() == null || request.getPurpose().isBlank()) {
            request.setPurpose(Purpose.INVENTORY.name());
        }
        inventoryService.createItems(request);
        return "redirect:/inventory?purpose=" + request.getPurpose();
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        TrackedItemResponse item = inventoryService.getById(id);
        model.addAttribute("page", "inventory");
        model.addAttribute("item", item);
        return "inventory/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, UpdateInventoryRequest request) {
        String purpose = inventoryService.update(id, request);
        return "redirect:/inventory?purpose=" + purpose;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        String purpose = inventoryService.getItemPurpose(id);
        inventoryService.delete(id);
        return "redirect:/inventory?purpose=" + purpose;
    }
}
