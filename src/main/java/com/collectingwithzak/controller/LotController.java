package com.collectingwithzak.controller;

import com.collectingwithzak.dto.common.MonthGroup;
import com.collectingwithzak.dto.request.LotRequest;
import com.collectingwithzak.dto.request.SnapshotItem;
import com.collectingwithzak.dto.response.LotResponse;
import com.collectingwithzak.entity.enums.LotAction;
import com.collectingwithzak.service.render.LotRenderService;
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

    private final LotRenderService lotRenderService;
    private final LotService lotService;

    @GetMapping
    public String renderIndex(Model model) {
        List<LotResponse> lots = lotRenderService.getAll();
        model.addAttribute("groups", MonthGroup.groupByMonth(lots, LotResponse::getPurchaseDate, LotResponse::getTotalCost));
        return "lots/index";
    }

    @GetMapping("/new")
    public String renderNewForm(Model model) {
        return "lots/new";
    }

    @GetMapping("/partials/row")
    public String rowPartial(SnapshotItem item, Model model) {
        model.addAttribute("item", item);
        return "lots/partials/row :: lot-row";
    }

    @GetMapping("/{id}")
    public String renderDetail(@PathVariable Long id, Model model) {
        LotResponse lot = lotRenderService.getById(id);
        model.addAttribute("lot", lot);
        model.addAttribute("snapshotItems", lot.getSnapshotItems());
        return "lots/detail";
    }

    @GetMapping("/{id}/edit")
    public String renderEditForm(@PathVariable Long id, Model model) {
        LotResponse lot = lotRenderService.getById(id);
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
