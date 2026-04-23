package com.collectingwithzak.controller;

import com.collectingwithzak.dto.SnapshotItem;
import com.collectingwithzak.dto.request.CreateLotRequest;
import com.collectingwithzak.dto.request.UpdateLotRequest;
import com.collectingwithzak.dto.response.LotResponse;
import com.collectingwithzak.service.LotService;
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

        model.addAttribute("lots", lotService.getAll());
        return "lots/index";
    }

    @GetMapping("/partials/row")
    public String rowPartial(@RequestParam(value = "type", defaultValue = "RAW_CARD") String type,
                             @RequestParam(defaultValue = "") String name,
                             @RequestParam(value = "set", defaultValue = "") String setName,
                             @RequestParam(value = "card", defaultValue = "") String cardNumber,
                             @RequestParam(defaultValue = "") String rarity,
                             @RequestParam(defaultValue = "0") double market,
                             @RequestParam(value = "img", defaultValue = "") String imageUrl,
                             @RequestParam(value = "card_id", defaultValue = "") String cardId,
                             Model model) {
        SnapshotItem item = new SnapshotItem();
        item.setName(name);
        item.setItemType(type);
        item.setSetName(setName);
        item.setCardNumber(cardNumber);
        item.setRarity(rarity);
        item.setMarketPrice(market);
        item.setImageUrl(imageUrl);
        item.setPokemonCardId(cardId);
        item.setQty(1);
        item.setPercentage(60);
        item.setTracked(false);
        model.addAttribute("item", item);
        return "lots/partials/row :: lot-row";
    }

    @GetMapping("/new")
    public String newForm(Model model) {

        return "lots/new";
    }

    @PostMapping
    public String create(CreateLotRequest request) {
        Long id = lotService.create(request);
        return "redirect:/lots/" + id;
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

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, UpdateLotRequest request) {
        lotService.update(id, request);
        return "redirect:/lots/" + id;
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam String action) {
        if ("accept".equals(action)) {
            lotService.accept(id);
        } else if ("reject".equals(action)) {
            lotService.reject(id);
        }
        return "redirect:/lots/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        lotService.delete(id);
        return "redirect:/lots";
    }
}
