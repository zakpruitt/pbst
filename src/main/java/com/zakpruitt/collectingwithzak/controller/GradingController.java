package com.zakpruitt.collectingwithzak.controller;

import com.zakpruitt.collectingwithzak.dto.common.MonthGroup;
import com.zakpruitt.collectingwithzak.dto.common.TrackedItemFilters;
import com.zakpruitt.collectingwithzak.dto.request.GradingItemRequest;
import com.zakpruitt.collectingwithzak.dto.request.GradingRequest;
import com.zakpruitt.collectingwithzak.entity.GradingSubmission;
import com.zakpruitt.collectingwithzak.entity.TrackedItem;
import com.zakpruitt.collectingwithzak.entity.enums.GradingAction;
import com.zakpruitt.collectingwithzak.entity.enums.ItemType;
import com.zakpruitt.collectingwithzak.service.GradingService;
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
@RequestMapping("/grading")
@RequiredArgsConstructor
public class GradingController {

    private final GradingService gradingService;

    @GetMapping
    public String renderIndex(Model model) {
        List<GradingSubmission> submissions = gradingService.getAll();
        model.addAttribute("groups", MonthGroup.groupByMonth(submissions, s -> s.getCreatedAt().toLocalDate()));
        return "grading/index";
    }

    @GetMapping("/new")
    public String renderNewForm(Model model) {
        List<TrackedItem> items = gradingService.getInventoryItems();
        model.addAttribute("rawItems", TrackedItemFilters.filterByType(items, ItemType.RAW_CARD));
        model.addAttribute("gradedItems", TrackedItemFilters.filterByType(items, ItemType.GRADED_CARD));
        return "grading/new";
    }

    @GetMapping("/{id}")
    public String renderDetail(@PathVariable Long id, Model model) {
        model.addAttribute("submission", gradingService.getByIdWithItems(id));
        return "grading/detail";
    }

    @GetMapping("/{id}/edit")
    public String renderEditForm(@PathVariable Long id, Model model) {
        GradingSubmission submission = gradingService.getByIdWithItems(id);
        List<TrackedItem> available = gradingService.getAvailableItemsFor(submission);
        Set<Long> attachedIds = submission.getItems().stream()
                .map(TrackedItem::getId)
                .collect(Collectors.toSet());

        model.addAttribute("submission", submission);
        model.addAttribute("rawItems", TrackedItemFilters.filterByType(available, ItemType.RAW_CARD));
        model.addAttribute("gradedItems", TrackedItemFilters.filterByType(available, ItemType.GRADED_CARD));
        model.addAttribute("attachedIds", attachedIds);
        return "grading/edit";
    }

    @PostMapping
    public String create(@Valid GradingRequest request) {
        Long id = gradingService.createWithItems(request);
        return "redirect:/grading/" + id;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid GradingRequest request) {
        gradingService.update(id, request);
        return "redirect:/grading/" + id;
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam("action") GradingAction action) {
        gradingService.updateStatus(id, action, null);
        return "redirect:/grading/" + id;
    }

    @PostMapping(value = "/{id}/status", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<Void> updateStatusJson(@PathVariable Long id,
                                                 @RequestParam("action") GradingAction action,
                                                 @Valid @RequestBody List<GradingItemRequest> grades) {
        gradingService.updateStatus(id, action, grades);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        gradingService.delete(id);
        return "redirect:/grading";
    }
}
