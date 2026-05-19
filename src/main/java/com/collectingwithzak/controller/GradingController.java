package com.collectingwithzak.controller;

import com.collectingwithzak.dto.common.MonthGroup;
import com.collectingwithzak.dto.grading.GradingItemRequest;
import com.collectingwithzak.dto.grading.GradingRequest;
import com.collectingwithzak.dto.grading.GradingSubmissionResponse;
import com.collectingwithzak.dto.inventory.TrackedItemFilters;
import com.collectingwithzak.dto.inventory.TrackedItemResponse;
import com.collectingwithzak.entity.enums.GradingAction;
import com.collectingwithzak.entity.enums.ItemType;
import com.collectingwithzak.service.GradingService;
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
    public String index(Model model) {
        List<GradingSubmissionResponse> submissions = gradingService.getAll();
        model.addAttribute("groups", MonthGroup.groupByMonth(submissions, s -> s.getCreatedAt().toLocalDate()));
        return "grading/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        List<TrackedItemResponse> items = gradingService.getInventoryItems();
        model.addAttribute("rawItems", TrackedItemFilters.filterByType(items, ItemType.RAW_CARD));
        model.addAttribute("gradedItems", TrackedItemFilters.filterByType(items, ItemType.GRADED_CARD));
        return "grading/new";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        GradingSubmissionResponse submission = gradingService.getByIdWithItems(id);
        model.addAttribute("submission", submission);
        return "grading/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        GradingSubmissionResponse submission = gradingService.getByIdWithItems(id);
        List<TrackedItemResponse> available = gradingService.getAvailableItemsForSubmission(id);
        Set<Long> attachedIds = submission.getItems().stream()
                .map(TrackedItemResponse::getId)
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
