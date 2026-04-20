package com.collectingwithzak.controller;

import com.collectingwithzak.dto.request.CreateGradingRequest;
import com.collectingwithzak.dto.request.ItemGradeRequest;
import com.collectingwithzak.dto.request.RecordReturnRequest;
import com.collectingwithzak.dto.request.UpdateGradingRequest;
import com.collectingwithzak.dto.response.MonthGroup;
import com.collectingwithzak.entity.GradingSubmission;
import com.collectingwithzak.entity.TrackedItem;
import com.collectingwithzak.service.GradingService;
import com.collectingwithzak.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/grading")
@RequiredArgsConstructor
public class GradingController {

    private final GradingService gradingService;
    private final InventoryService inventoryService;

    @GetMapping
    public String index(Model model) {
        List<GradingSubmission> submissions = gradingService.getAll();
        model.addAttribute("page", "grading");
        model.addAttribute("groups", MonthGroup.groupByMonth(submissions, s -> s.getCreatedAt().toLocalDate()));
        return "grading/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        List<TrackedItem> items = inventoryService.getByPurpose("INVENTORY");
        List<TrackedItem> raw = new ArrayList<>();
        List<TrackedItem> graded = new ArrayList<>();
        for (TrackedItem item : items) {
            if ("GRADED_CARD".equals(item.getItemType())) graded.add(item);
            else if ("RAW_CARD".equals(item.getItemType())) raw.add(item);
        }

        model.addAttribute("page", "grading");
        model.addAttribute("rawItems", raw);
        model.addAttribute("gradedItems", graded);
        return "grading/new";
    }

    @PostMapping
    public String create(CreateGradingRequest request) {
        GradingSubmission submission = gradingService.createWithItems(
                request.getCompany(),
                request.getSubmissionMethod(),
                request.getSubmissionCost(),
                request.getNotes(),
                request.getItemIds() != null ? request.getItemIds() : List.of()
        );
        return "redirect:/grading/" + submission.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        GradingSubmission submission = gradingService.getById(id);
        model.addAttribute("page", "grading");
        model.addAttribute("submission", submission);
        return "grading/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        GradingSubmission submission = gradingService.getById(id);
        Set<Long> attachedIds = submission.getItems().stream()
                .map(TrackedItem::getId)
                .collect(Collectors.toSet());

        List<TrackedItem> inventoryItems = inventoryService.getByPurpose("INVENTORY");
        List<TrackedItem> allItems = new ArrayList<>(inventoryItems);
        allItems.addAll(submission.getItems());

        List<TrackedItem> raw = new ArrayList<>();
        List<TrackedItem> graded = new ArrayList<>();
        for (TrackedItem item : allItems) {
            if ("GRADED_CARD".equals(item.getItemType())) graded.add(item);
            else if ("RAW_CARD".equals(item.getItemType())) raw.add(item);
        }

        model.addAttribute("page", "grading");
        model.addAttribute("submission", submission);
        model.addAttribute("rawItems", raw);
        model.addAttribute("gradedItems", graded);
        model.addAttribute("attachedIds", attachedIds);
        return "grading/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, UpdateGradingRequest request) {
        gradingService.update(
                id,
                request.getCompany(),
                request.getSubmissionMethod(),
                request.getSubmissionCost(),
                request.getNotes(),
                request.getItemIds() != null ? request.getItemIds() : List.of()
        );
        return "redirect:/grading/" + id;
    }

    @PostMapping("/{id}/advance")
    public String advanceStatus(@PathVariable Long id, @RequestParam("new_status") String newStatus) {
        gradingService.advanceStatus(id, newStatus);
        return "redirect:/grading/" + id;
    }

    @PostMapping("/{id}/return")
    public String recordReturn(@PathVariable Long id, @RequestBody RecordReturnRequest request) {
        gradingService.recordReturn(id, request.getGrades());
        return "redirect:/grading/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        gradingService.delete(id);
        return "redirect:/grading";
    }

}
