package com.collectingwithzak.controller;

import com.collectingwithzak.dto.common.MonthGroup;
import com.collectingwithzak.dto.lot.SnapshotItem;
import com.collectingwithzak.dto.lot.LotRequest;
import com.collectingwithzak.dto.lot.LotResponse;
import com.collectingwithzak.service.LotService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/lots")
@RequiredArgsConstructor
public class LotController {

    private final LotService lotService;

    @GetMapping
    public String index(Model model) {
        List<LotResponse> lots = lotService.getAll();
        model.addAttribute("groups", MonthGroup.groupByMonth(lots, LotResponse::getPurchaseDate, LotResponse::getTotalCost));
        return "lots/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        return "lots/new";
    }

    @GetMapping("/partials/row")
    public String rowPartial(SnapshotItem item, Model model) {
        model.addAttribute("item", item);
        return "lots/partials/row :: lot-row";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        LotResponse lot = lotService.getById(id);
        model.addAttribute("lot", lot);
        model.addAttribute("snapshotItems", lot.getSnapshotItems());
        return "lots/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        LotResponse lot = lotService.getById(id);
        model.addAttribute("lot", lot);
        model.addAttribute("snapshotItems", lot.getSnapshotItems());
        return "lots/edit";
    }

    @PostMapping
    public String create(@Valid LotRequest request) {
        Long id = lotService.create(request);
        return "redirect:/lots/" + id;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid LotRequest request) {
        lotService.update(id, request);
        return "redirect:/lots/" + id;
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam String action) {
        lotService.updateStatus(id, action);
        return "redirect:/lots/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        lotService.delete(id);
        return "redirect:/lots";
    }
}
