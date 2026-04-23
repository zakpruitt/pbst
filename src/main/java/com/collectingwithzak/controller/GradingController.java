package com.collectingwithzak.controller;

import com.collectingwithzak.dto.request.CreateGradingRequest;
import com.collectingwithzak.dto.request.RecordReturnRequest;
import com.collectingwithzak.dto.request.UpdateGradingRequest;
import com.collectingwithzak.dto.response.GradingFormData;
import com.collectingwithzak.dto.response.GradingSubmissionResponse;
import com.collectingwithzak.dto.response.MonthGroup;
import com.collectingwithzak.service.GradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        GradingFormData formData = gradingService.getNewFormData();

        model.addAttribute("rawItems", formData.getRawItems());
        model.addAttribute("gradedItems", formData.getGradedItems());
        return "grading/new";
    }

    @PostMapping
    public String create(CreateGradingRequest request) {
        Long id = gradingService.createWithItems(request);
        return "redirect:/grading/" + id;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        GradingSubmissionResponse submission = gradingService.getByIdWithItems(id);

        model.addAttribute("submission", submission);
        return "grading/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        GradingFormData formData = gradingService.getEditFormData(id);

        model.addAttribute("submission", formData.getSubmission());
        model.addAttribute("rawItems", formData.getRawItems());
        model.addAttribute("gradedItems", formData.getGradedItems());
        model.addAttribute("attachedIds", formData.getAttachedIds());
        return "grading/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, UpdateGradingRequest request) {
        gradingService.update(request, id);
        return "redirect:/grading/" + id;
    }

    @PostMapping("/{id}/advance")
    public String advanceStatus(@PathVariable Long id, @RequestParam("new_status") String newStatus) {
        gradingService.advanceStatus(id, newStatus);
        return "redirect:/grading/" + id;
    }

    @PostMapping("/{id}/return")
    @ResponseBody
    public ResponseEntity<Void> recordReturn(@PathVariable Long id, @RequestBody RecordReturnRequest request) {
        gradingService.recordReturn(id, request.getGrades());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        gradingService.delete(id);
        return "redirect:/grading";
    }

}
