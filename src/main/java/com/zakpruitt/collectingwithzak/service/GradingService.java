package com.zakpruitt.collectingwithzak.service;

import com.zakpruitt.collectingwithzak.dto.request.GradingItemRequest;
import com.zakpruitt.collectingwithzak.dto.request.GradingRequest;
import com.zakpruitt.collectingwithzak.entity.GradingSubmission;
import com.zakpruitt.collectingwithzak.entity.TrackedItem;
import com.zakpruitt.collectingwithzak.entity.enums.GradingAction;
import com.zakpruitt.collectingwithzak.entity.enums.GradingStatus;
import com.zakpruitt.collectingwithzak.entity.enums.ItemStatus;
import com.zakpruitt.collectingwithzak.mapper.GradedDetailsMapper;
import com.zakpruitt.collectingwithzak.mapper.GradingMapper;
import com.zakpruitt.collectingwithzak.repository.GradingSubmissionRepository;
import com.zakpruitt.collectingwithzak.repository.TrackedItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static com.zakpruitt.collectingwithzak.exception.ResourceNotFoundException.notFound;

@Service
@RequiredArgsConstructor
@Transactional
public class GradingService {

    private final GradingSubmissionRepository gradingRepo;
    private final TrackedItemRepository itemRepo;
    private final GradingMapper gradingMapper;
    private final GradedDetailsMapper gradedDetailsMapper;

    @Transactional(readOnly = true)
    public List<GradingSubmission> getAll() {
        return gradingRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public GradingSubmission getByIdWithItems(Long id) {
        return gradingRepo.findWithItemsById(id).orElseThrow(notFound("GradingSubmission", id));
    }

    @Transactional(readOnly = true)
    public List<TrackedItem> getInventoryItems() {
        return itemRepo.findByStatus(ItemStatus.AVAILABLE);
    }

    @Transactional(readOnly = true)
    public List<TrackedItem> getAvailableItemsFor(GradingSubmission submission) {
        return itemRepo.findAvailablePlus(submission.getItems());
    }

    public Long createWithItems(GradingRequest request) {
        long count = gradingRepo.countByCompany(request.getCompany());

        GradingSubmission submission = gradingMapper.toEntity(request);
        submission.setSubmissionName(String.format("%s Submission #%d", request.getCompany(), count + 1));
        gradingRepo.save(submission);

        itemRepo.findAllById(request.getItemIds()).forEach(item -> item.attachTo(submission));
        return submission.getId();
    }

    public void update(Long id, GradingRequest request) {
        GradingSubmission submission = getByIdWithItems(id);
        submission.getItems().forEach(TrackedItem::releaseFromGrading);
        itemRepo.findAllById(request.getItemIds()).forEach(item -> item.attachTo(submission));
        gradingMapper.updateEntity(request, submission);
    }

    public void updateStatus(Long id, GradingAction action, List<GradingItemRequest> grades) {
        GradingSubmission submission = gradingRepo.findById(id)
                                                  .orElseThrow(notFound("GradingSubmission", id));
        switch (action) {
            case SEND -> {
                submission.setStatus(GradingStatus.IN_GRADING);
                submission.setSendDate(LocalDate.now());
            }
            case RETURN -> recordReturn(submission, grades);
        }
    }

    public void delete(Long id) {
        GradingSubmission submission = getByIdWithItems(id);
        submission.getItems().forEach(TrackedItem::releaseFromGrading);
        gradingRepo.delete(submission);
    }

    private void recordReturn(GradingSubmission submission, List<GradingItemRequest> grades) {
        double totalUpcharge = 0;
        for (GradingItemRequest grade : grades) {
            TrackedItem item = itemRepo.findById(grade.getItemId())
                                       .orElseThrow(notFound("TrackedItem", grade.getItemId()));
            item.returnFromGrading(gradedDetailsMapper.fromGradeRequest(grade, submission.getCompany()));
            totalUpcharge += grade.getUpcharge();
        }

        submission.setUpchargeTotal(totalUpcharge);
        submission.setReturnDate(LocalDate.now());
        submission.setStatus(GradingStatus.RETURNED);
    }
}
