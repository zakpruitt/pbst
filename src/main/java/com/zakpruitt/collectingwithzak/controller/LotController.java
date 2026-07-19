package com.zakpruitt.collectingwithzak.controller;

import com.zakpruitt.collectingwithzak.dto.common.MonthGroup;
import com.zakpruitt.collectingwithzak.dto.request.LotRequest;
import com.zakpruitt.collectingwithzak.dto.request.SnapshotItem;
import com.zakpruitt.collectingwithzak.entity.LotPurchase;
import com.zakpruitt.collectingwithzak.entity.enums.LotAction;
import com.zakpruitt.collectingwithzak.service.LotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/lots")
@RequiredArgsConstructor
public class LotController {

    private final LotService lotService;

    @GetMapping
    public String renderIndex(Model model) {
        List<LotPurchase> lots = lotService.getAll();
        model.addAttribute("groups", MonthGroup.groupByMonth(lots, LotPurchase::getPurchaseDate, LotPurchase::getTotalCost));
        return "lots/index";
    }

    @GetMapping("/new")
    public String renderNewForm() {
        return "lots/new";
    }

    @GetMapping("/partials/row")
    public String rowPartial(SnapshotItem item, Model model) {
        model.addAttribute("item", item);
        return "lots/partials/row :: lot-row";
    }

    @GetMapping("/{id}")
    public String renderDetail(@PathVariable Long id, Model model) {
        LotPurchase lot = lotService.getByIdWithItems(id);
        model.addAttribute("lot", lot);
        model.addAttribute("snapshotItems", lot.parseSnapshot());
        return "lots/detail";
    }

    @GetMapping("/{id}/edit")
    public String renderEditForm(@PathVariable Long id, Model model) {
        LotPurchase lot = lotService.getById(id);
        model.addAttribute("lot", lot);
        model.addAttribute("snapshotItems", lot.parseSnapshot());
        return "lots/edit";
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<String> create(@RequestBody @Valid LotRequest request) {
        Long id = lotService.create(request);
        return ResponseEntity.ok("/lots/" + id);
    }

    @PostMapping("/{id}")
    @ResponseBody
    public ResponseEntity<String> update(@PathVariable Long id, @RequestBody @Valid LotRequest request) {
        lotService.update(id, request);
        return ResponseEntity.ok("/lots/" + id);
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam LotAction action) {
        lotService.updateStatus(id, action);
        return "redirect:/lots/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        lotService.delete(id);
        return "redirect:/lots";
    }
}
