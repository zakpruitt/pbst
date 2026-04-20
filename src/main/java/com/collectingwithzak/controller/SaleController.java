package com.collectingwithzak.controller;

import com.collectingwithzak.dto.request.CreateSaleRequest;
import com.collectingwithzak.dto.response.MonthGroup;
import com.collectingwithzak.entity.Sale;
import com.collectingwithzak.entity.TrackedItem;
import com.collectingwithzak.service.InventoryService;
import com.collectingwithzak.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;
    private final InventoryService inventoryService;

    @GetMapping
    public String index(@RequestParam(defaultValue = "mine") String view, Model model) {
        if (!Set.of("mine", "ignored", "vince").contains(view)) {
            view = "mine";
        }

        List<Sale> sales = saleService.getAll(view);
        long stagedCount = saleService.countStaged();

        model.addAttribute("page", "sales");
        model.addAttribute("groups", MonthGroup.groupByMonth(sales, Sale::getSaleDate));
        model.addAttribute("stagedCount", stagedCount);
        model.addAttribute("view", view);
        return "sales/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("page", "sales");
        return "sales/new";
    }

    @GetMapping("/staging")
    public String staging(Model model) {
        model.addAttribute("page", "sales");
        model.addAttribute("sales", saleService.getStaged());
        return "sales/staging";
    }

    @PostMapping
    public String create(CreateSaleRequest request) {
        saleService.create(request);
        return "redirect:/sales";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Sale sale = saleService.getById(id);
        model.addAttribute("page", "sales");
        model.addAttribute("sale", sale);
        return "sales/detail";
    }

    @GetMapping("/{id}/confirm")
    public String confirmForm(@PathVariable Long id, Model model) {
        Sale sale = saleService.getById(id);
        Set<Long> attachedIds = sale.getItems().stream()
                .map(TrackedItem::getId)
                .collect(Collectors.toSet());

        List<TrackedItem> allItems = inventoryService.getInventoryForSaleConfirm(sale);
        List<TrackedItem> raw = new ArrayList<>();
        List<TrackedItem> graded = new ArrayList<>();
        for (TrackedItem item : allItems) {
            if ("GRADED_CARD".equals(item.getItemType())) graded.add(item);
            else if ("RAW_CARD".equals(item.getItemType())) raw.add(item);
        }

        model.addAttribute("page", "sales");
        model.addAttribute("sale", sale);
        model.addAttribute("rawItems", raw);
        model.addAttribute("gradedItems", graded);
        model.addAttribute("attachedIds", attachedIds);
        return "sales/confirm";
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable Long id, @RequestParam(name = "item_ids", required = false) List<Long> itemIds) {
        saleService.confirmWithItems(id, itemIds != null ? itemIds : List.of());
        return "redirect:/sales/" + id;
    }

    @PostMapping("/{id}/ignore")
    public String ignore(@PathVariable Long id) {
        saleService.ignore(id);
        return "redirect:/sales/staging";
    }

    @PostMapping("/{id}/vince")
    public String vince(@PathVariable Long id, @RequestParam(name = "from", required = false) String from) {
        saleService.markAsVince(id);
        if ("detail".equals(from)) {
            return "redirect:/sales?view=vince";
        }
        return "redirect:/sales/staging";
    }

    @PostMapping("/{id}/unstage")
    public String unstage(@PathVariable Long id) {
        saleService.unstage(id);
        return "redirect:/sales/staging";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        saleService.delete(id);
        return "redirect:/sales";
    }

}
