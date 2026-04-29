package com.collectingwithzak.controller;

import com.collectingwithzak.dto.request.CreateSaleRequest;
import com.collectingwithzak.dto.request.CreateVincePaymentRequest;
import com.collectingwithzak.dto.response.MonthGroup;
import com.collectingwithzak.dto.response.SaleConfirmFormData;
import com.collectingwithzak.dto.response.SaleResponse;
import com.collectingwithzak.service.SaleService;
import com.collectingwithzak.service.VincePaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

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

        List<SaleResponse> sales = saleService.getAll(view);
        long stagedCount = saleService.countStaged();


        List<MonthGroup<SaleResponse>> groups = MonthGroup.groupByMonth(sales, SaleResponse::getSaleDate);
        MonthGroup.computeSubtotals(groups, SaleResponse::getNetAmount);
        model.addAttribute("groups", groups);
        model.addAttribute("stagedCount", stagedCount);
        model.addAttribute("view", view);

        if ("vince".equals(view)) {
            model.addAttribute("vinceLedger", vincePaymentService.getLedger());
            model.addAttribute("vincePayments", vincePaymentService.getAll());
        }

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

    @PostMapping
    public String create(CreateSaleRequest request) {
        saleService.create(request);
        return "redirect:/sales";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        SaleResponse sale = saleService.getByIdWithItems(id);

        model.addAttribute("sale", sale);
        return "sales/detail";
    }

    @GetMapping("/{id}/confirm")
    public String confirmForm(@PathVariable Long id, @RequestParam(name = "from", required = false) String from, Model model) {
        SaleConfirmFormData formData = saleService.getConfirmFormData(id);

        model.addAttribute("sale", formData.getSale());
        model.addAttribute("rawItems", formData.getRawItems());
        model.addAttribute("gradedItems", formData.getGradedItems());
        model.addAttribute("attachedIds", formData.getAttachedIds());
        model.addAttribute("from", from != null ? from : "");
        return "sales/confirm";
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

    @PostMapping("/{id}/ignore")
    public Object ignore(@PathVariable Long id, @RequestHeader(value = "HX-Request", required = false) String hx) {
        saleService.ignore(id);
        if (hx != null) {
            return ResponseEntity.ok("");
        }
        return "redirect:/sales/staging";
    }

    @PostMapping("/{id}/vince")
    public Object vince(@PathVariable Long id,
                        @RequestParam(name = "from", required = false) String from,
                        @RequestHeader(value = "HX-Request", required = false) String hx) {
        saleService.markAsVince(id);
        if (hx != null) {
            return ResponseEntity.ok("");
        }
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

    @PostMapping("/vince/payments")
    public String createVincePayment(CreateVincePaymentRequest request) {
        vincePaymentService.create(request);
        return "redirect:/sales?view=vince";
    }

    @PostMapping("/vince/payments/{id}/delete")
    public String deleteVincePayment(@PathVariable Long id) {
        vincePaymentService.delete(id);
        return "redirect:/sales?view=vince";
    }

}
