package com.zakpruitt.pbst.controllers;

import com.zakpruitt.pbst.dtos.GradingSubmissionDTO;
import com.zakpruitt.pbst.enums.SubmissionStatus;
import com.zakpruitt.pbst.mappers.GradingSubmissionMapper;
import com.zakpruitt.pbst.services.GradingSubmissionService;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/grading-submissions")
@AllArgsConstructor
public class GradingSubmissionController {

    private final GradingSubmissionService gradingSubmissionService;
    private final GradingSubmissionMapper gradingSubmissionMapper;

    @GetMapping
    public ResponseEntity<List<GradingSubmissionDTO>> getAllSubmissions() {
        return ResponseEntity.ok(gradingSubmissionService.getAllSubmissions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GradingSubmissionDTO> getSubmissionById(@PathVariable Long id) {
        return ResponseEntity.ok(gradingSubmissionService.getSubmissionById(id));
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> createSubmission(@ModelAttribute GradingSubmissionDTO dto) {
        GradingSubmissionDTO savedSubmission = gradingSubmissionService.saveSubmission(dto);
        return ResponseEntity.status(303).location(URI.create("/grading/" + savedSubmission.getId())).build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestParam SubmissionStatus status) {
        gradingSubmissionService.updateStatus(id, status);
        // For HTMX, we might want to return a fragment, but for now a redirect to details is safe
        return ResponseEntity.status(303).location(URI.create("/grading/" + id)).build();
    }

    @PostMapping(value = "/finalize", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> finalizeSubmission(@ModelAttribute GradingSubmissionDTO dto) {
        // We need a service method to handle the bulk update of items + submission status
        gradingSubmissionService.finalizeSubmission(dto);
        return ResponseEntity.status(303).location(URI.create("/grading/" + dto.getId())).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubmission(@PathVariable Long id) {
        gradingSubmissionService.deleteSubmission(id);
        return ResponseEntity.noContent().build();
    }
}
