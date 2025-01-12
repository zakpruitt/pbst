package com.zakpruitt.pbst.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zakpruitt.pbst.dtos.LotPurchaseDTO;
import com.zakpruitt.pbst.dtos.PokemonCardSearchDTO;
import com.zakpruitt.pbst.dtos.TrackedItemDTO;
import com.zakpruitt.pbst.enums.ItemGradingStatus;
import com.zakpruitt.pbst.enums.Purpose;
import com.zakpruitt.pbst.services.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@AllArgsConstructor
public class ViewController {

    private final LotPurchaseService lotPurchaseService;
    private final PokemonCardService pokemonCardService;
    private final SaleService saleService;
    private final TrackedItemService trackedItemService;
    private final GradingSubmissionService gradingSubmissionService;
    private final ObjectMapper objectMapper;

    @GetMapping("/")
    public String dashboard(Model model) {
        BigDecimal totalSpent = lotPurchaseService.getTotalCost();
        BigDecimal totalNet = saleService.getTotalNet();
        
        if (totalSpent == null) totalSpent = BigDecimal.ZERO;
        if (totalNet == null) totalNet = BigDecimal.ZERO;

        // 1. Total Spent (All)
        model.addAttribute("totalSpent", totalSpent);
        
        // 2. Total Spent (Lots Only) - Assuming same as totalSpent for now
        model.addAttribute("totalLotCost", totalSpent);

        // 3. Net Lot Cost (Factoring in Keeps)
        BigDecimal collectionCost = trackedItemService.getCostBasisByPurpose(Purpose.PERSONAL_COLLECTION);
        BigDecimal inventoryCost = trackedItemService.getCostBasisByPurpose(Purpose.GRADED_INVENTORY);
        BigDecimal toGradeCost = trackedItemService.getCostBasisByPurpose(Purpose.TO_GRADE);
        BigDecimal inGradingCost = trackedItemService.getCostBasisByPurpose(Purpose.IN_GRADING);
        
        BigDecimal totalKeepsCost = collectionCost.add(inventoryCost).add(toGradeCost).add(inGradingCost);
        BigDecimal netLotCost = totalSpent.subtract(totalKeepsCost);
        model.addAttribute("netLotCost", netLotCost);

        // 4. Value in Grading
        BigDecimal valueInGrading = trackedItemService.getMarketValueByGradingStatus(ItemGradingStatus.IN_GRADING);
        model.addAttribute("valueInGrading", valueInGrading);

        // 5. Unrealized Profit (Inventory)
        BigDecimal inventoryMarketValue = trackedItemService.getUnsoldMarketValueByPurpose(Purpose.GRADED_INVENTORY);
        BigDecimal unrealizedProfit = inventoryMarketValue.subtract(inventoryCost);
        model.addAttribute("unrealizedInventoryProfit", unrealizedProfit);
        model.addAttribute("inventoryMarketValue", inventoryMarketValue);

        // 6. Sales
        model.addAttribute("totalNetSales", totalNet);
        model.addAttribute("netProfit", totalNet.subtract(netLotCost)); // Profit against the "flip" portion of cost

        // Counts
        model.addAttribute("collectionCount", trackedItemService.countByPurpose(Purpose.PERSONAL_COLLECTION));
        model.addAttribute("gradingCount", trackedItemService.countByPurpose(Purpose.TO_GRADE) + trackedItemService.countByPurpose(Purpose.IN_GRADING));
        model.addAttribute("inventoryCount", trackedItemService.countByPurpose(Purpose.GRADED_INVENTORY));
        
        model.addAttribute("unrealizedValue", trackedItemService.getUnsoldMarketValue());

        return "dashboard";
    }

    @GetMapping("/lots")
    public String lots(Model model) {
        model.addAttribute("lots", lotPurchaseService.getAllLots());
        return "lots/index";
    }

    @GetMapping("/lots/new")
    public String newLot(Model model) {
        model.addAttribute("today", LocalDate.now());
        return "lots/new";
    }

    @GetMapping("/lots/{id}")
    public String lotDetails(@PathVariable Long id, Model model) {
        model.addAttribute("lot", lotPurchaseService.getLotById(id));
        return "lots/details";
    }

    @GetMapping("/lots/{id}/edit")
    public String editLot(@PathVariable Long id, Model model) {
        LotPurchaseDTO lot = lotPurchaseService.getLotById(id);
        model.addAttribute("lot", lot);

        if (lot.getLotContentSnapshot() != null && !lot.getLotContentSnapshot().isEmpty()) {
            try {
                List<Map<String, Object>> snapshotItems = objectMapper.readValue(lot.getLotContentSnapshot(), new TypeReference<>() {});
                model.addAttribute("snapshotItems", snapshotItems);
            } catch (Exception e) {
                model.addAttribute("snapshotItems", Collections.emptyList());
            }
        } else {
            model.addAttribute("snapshotItems", Collections.emptyList());
        }

        return "lots/edit";
    }

    @PatchMapping("/lots/{id}/status")
    public String updateLotStatus(@PathVariable Long id, @RequestParam String status, Model model) {
        lotPurchaseService.updateStatus(id, status);
        model.addAttribute("lot", lotPurchaseService.getLotById(id));
        return "lots/details :: lotHeader";
    }

    // HTMX partial for card search results
    @GetMapping("/search/cards")
    public String searchCards(@RequestParam String query, Model model) {
        model.addAttribute("cards", pokemonCardService.searchCards(query));
        return "fragments/card-search-results";
    }

    // HTMX endpoint to add a new tracked item form row
    @PostMapping("/lots/add-comp-item")
    public String addCompItemRow(
            @RequestParam(name = "itemIndex") int itemIndex,
            @RequestParam(name = "pokemonCardId", required = false) String pokemonCardId,
            Model model) {

        if (pokemonCardId != null && !pokemonCardId.isEmpty()) {
            PokemonCardSearchDTO card = pokemonCardService.getPokemonCardSearchDTOById(pokemonCardId);
            model.addAttribute("card", card);
        }

        model.addAttribute("itemIndex", itemIndex);
        return "fragments/comp-item-row";
    }
    
    @GetMapping("/sales")
    public String sales(Model model) {
        model.addAttribute("sales", saleService.getAllSales());
        return "sales/index";
    }

    @GetMapping("/grading")
    public String grading(Model model) {
        model.addAttribute("submissions", gradingSubmissionService.getAllSubmissions());
        model.addAttribute("activeCount", gradingSubmissionService.countActiveSubmissions());
        model.addAttribute("totalSpent", gradingSubmissionService.getTotalGradingCost());
        return "grading/index";
    }

    @GetMapping("/grading/new")
    public String newGradingSubmission(Model model) {
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("itemsToGrade", trackedItemService.getItemsByGradingStatus(ItemGradingStatus.TO_GRADE));
        return "grading/new";
    }

    @GetMapping("/grading/{id}")
    public String gradingDetails(@PathVariable Long id, Model model) {
        model.addAttribute("submission", gradingSubmissionService.getSubmissionById(id));
        return "grading/details";
    }

    @GetMapping("/grading/{id}/results")
    public String gradingResults(@PathVariable Long id, Model model) {
        model.addAttribute("submission", gradingSubmissionService.getSubmissionById(id));
        return "grading/results";
    }

    @GetMapping("/collection")
    public String collection(@RequestParam(required = false) Purpose purpose, Model model) {
        List<TrackedItemDTO> items = trackedItemService.getAllTrackedItems();
        
        if (purpose != null) {
             items = items.stream()
                     .filter(i -> i.getPurpose() == purpose)
                     .collect(Collectors.toList());
        }
        
        model.addAttribute("items", items);
        model.addAttribute("currentPurpose", purpose);
        return "collection/index";
    }
}
