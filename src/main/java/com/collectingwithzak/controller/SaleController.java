package com.collectingwithzak.controller;

import com.collectingwithzak.dto.inventory.TrackedItemFilters;
import com.collectingwithzak.dto.sale.CreateSaleRequest;
import com.collectingwithzak.dto.sale.SaleResponse;
import com.collectingwithzak.dto.inventory.TrackedItemResponse;
import com.collectingwithzak.dto.vince.CreateVincePaymentRequest;
import com.collectingwithzak.entity.enums.ItemType;
import com.collectingwithzak.entity.enums.SaleAction;
import com.collectingwithzak.service.SaleService;
import com.collectingwithzak.service.VincePaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;
    private final VincePaymentService vincePaymentService;

    @GetMapping
    public String index(@RequestParam(defaultValue = "mine") String view, Model model) {
        if (!Set.of("mine", "ignored", "vince").contains(view)) {
            view = "mine";
        }
        model.addAttribute("data", saleService.getIndexData(view));
        return "sales/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        return "sales/new";
    }

    @GetMapping("/staging")
    public String staging(Model model) {
        model.addAttribute("sales", saleService.getStaged());
        return "sales/staging";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        SaleResponse sale = saleService.getByIdWithItems(id);
        model.addAttribute("sale", sale);
        return "sales/detail";
    }

    @GetMapping("/{id}/confirm")
    public String confirmForm(@PathVariable Long id, @RequestParam(name = "from", required = false) String from, Model model) {
        SaleResponse sale = saleService.getByIdWithItems(id);
    List<TrackedItemResponse> available = saleService.getAvailableItemsForSale(id);
        Set<Long> attachedIds = sale.getItems().stream()
                .map(TrackedItemResponse::getId)
                .collect(Collectors.toSet());

        model.addAttribute("sale", sale);
        model.addAttribute("rawItems", TrackedItemFilters.filterByType(available, ItemType.RAW_CARD));
        model.addAttribute("gradedItems", TrackedItemFilters.filterByType(available, ItemType.GRADED_CARD));
        model.addAttribute("attachedIds", attachedIds);
        model.addAttribute("from", from != null ? from : "");
        return "sales/confirm";
    }

    @PostMapping
    public String create(@Valid CreateSaleRequest request) {
        saleService.create(request);
        return "redirect:/sales";
    }

    @PostMapping("/vince/payments")
    public String createVincePayment(@Valid CreateVincePaymentRequest request) {
        vincePaymentService.create(request);
        return "redirect:/sales?view=vince";
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable Long id,
                          @RequestParam(name = "item_ids", required = false) List<Long> itemIds,
                          @RequestParam(name = "from", required = false) String from) {
        saleService.confirmWithItems(id, itemIds != null ? itemIds : List.of());
        if ("staging".equals(from)) {
            return "redirect:/sales/staging";
        }
        return "redirect:/sales/" + id;
    }

    @PostMapping("/{id}/status")
    public Object updateStatus(@PathVariable Long id,
                               @RequestParam("action") SaleAction action,
                               @RequestParam(name = "from", required = false) String from,
                               @RequestHeader(value = "HX-Request", required = false) String hx) {
        saleService.updateStatus(id, action);
        if (hx != null) {
            return ResponseEntity.noContent().build();
        }
        if (action == SaleAction.VINCE && "detail".equals(from)) {
            return "redirect:/sales?view=vince";
        }
        return "redirect:/sales/staging";
    }

    @PatchMapping("/{id}/amounts")
    @ResponseBody
    public ResponseEntity<Void> updateAmounts(@PathVariable Long id,
                                              @RequestParam("grossAmount") double grossAmount,
                                              @RequestParam("netAmount") double netAmount) {
        saleService.updateAmounts(id, grossAmount, netAmount);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        saleService.delete(id);
        return "redirect:/sales";
    }

    @PostMapping("/vince/payments/{id}/delete")
    public String deleteVincePayment(@PathVariable Long id) {
        vincePaymentService.delete(id);
        return "redirect:/sales?view=vince";
    }

}
