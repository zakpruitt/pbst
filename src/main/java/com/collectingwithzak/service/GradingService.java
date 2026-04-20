package com.collectingwithzak.service;

import com.collectingwithzak.dto.request.ItemGradeRequest;
import com.collectingwithzak.entity.GradedDetails;
import com.collectingwithzak.entity.GradingSubmission;
import com.collectingwithzak.exception.ResourceNotFoundException;
import com.collectingwithzak.repository.GradingSubmissionRepository;
import com.collectingwithzak.repository.TrackedItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GradingService {

    private final GradingSubmissionRepository gradingRepo;
    private final TrackedItemRepository itemRepo;

    public GradingSubmission createWithItems(String company, String method, double submissionCost, String notes, List<Long> itemIds) {
        long count = gradingRepo.countByCompany(company);
        double costPerCard = itemIds.isEmpty() ? 0 : submissionCost / itemIds.size();

        GradingSubmission submission = new GradingSubmission();
        submission.setSubmissionName(String.format("%s Submission #%d", company, count + 1));
        submission.setCompany(company);
        submission.setSubmissionMethod(method);
        submission.setStatus("PREPPING");
        submission.setCostPerCard(costPerCard);
        submission.setSubmissionCost(submissionCost);
        submission.setNotes(notes);

        submission = gradingRepo.save(submission);

        if (!itemIds.isEmpty()) {
            itemRepo.attachToSubmission(itemIds, submission.getId());
            itemRepo.updatePurposeBySubmission(submission.getId(), "IN_GRADING");
        }

        return submission;
    }

    @Transactional(readOnly = true)
    public List<GradingSubmission> getAll() {
        return gradingRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public GradingSubmission getById(Long id) {
        return gradingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GradingSubmission", id));
    }

    public void update(Long id, String company, String method, double submissionCost, String notes, List<Long> itemIds) {
        GradingSubmission submission = getById(id);

        itemRepo.detachFromSubmission(id);

        if (itemIds != null && !itemIds.isEmpty()) {
            itemRepo.attachToSubmission(itemIds, id);
            itemRepo.updatePurposeBySubmission(id, "IN_GRADING");
        }

        double costPerCard = (itemIds == null || itemIds.isEmpty()) ? 0 : submissionCost / itemIds.size();
        submission.setCompany(company);
        submission.setSubmissionMethod(method);
        submission.setSubmissionCost(submissionCost);
        submission.setCostPerCard(costPerCard);
        submission.setNotes(notes);

        gradingRepo.save(submission);
    }

    public void delete(Long id) {
        itemRepo.detachFromSubmission(id);
        gradingRepo.deleteById(id);
    }

    public void advanceStatus(Long id, String newStatus) {
        gradingRepo.updateStatus(id, newStatus);
        if ("IN_TRANSIT".equals(newStatus)) {
            gradingRepo.setSendDate(id, LocalDate.now());
        }
    }

    public void recordReturn(Long submissionId, List<ItemGradeRequest> grades) {
        GradingSubmission submission = getById(submissionId);

        double totalUpcharge = 0;
        for (ItemGradeRequest g : grades) {
            GradedDetails details = new GradedDetails(submission.getCompany(), g.getGrade(), g.getUpcharge());
            var item = itemRepo.findById(g.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("TrackedItem", g.getItemId()));
            item.setGradedDetails(details);
            item.setPurpose("INVENTORY");
            item.setItemType("GRADED_CARD");
            itemRepo.save(item);
            totalUpcharge += g.getUpcharge();
        }

        gradingRepo.updateReturnDetails(submissionId, totalUpcharge, LocalDate.now());
        gradingRepo.updateStatus(submissionId, "RETURNED");
    }
}
