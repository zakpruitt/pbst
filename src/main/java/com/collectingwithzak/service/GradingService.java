package com.collectingwithzak.service;

import com.collectingwithzak.dto.request.GradingItemRequest;
import com.collectingwithzak.dto.request.GradingRequest;
import com.collectingwithzak.entity.GradingSubmission;
import com.collectingwithzak.entity.TrackedItem;
import com.collectingwithzak.entity.enums.GradingAction;
import com.collectingwithzak.entity.enums.GradingStatus;
import com.collectingwithzak.entity.enums.ItemStatus;
import com.collectingwithzak.entity.enums.ItemType;
import com.collectingwithzak.exception.ResourceNotFoundException;
import com.collectingwithzak.mapper.GradedDetailsMapper;
import com.collectingwithzak.mapper.GradingMapper;
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
    private final GradingMapper gradingMapper;
    private final GradedDetailsMapper gradedDetailsMapper;

    public Long createWithItems(GradingRequest request) {
        List<Long> itemIds = request.getItemIds();
        long count = gradingRepo.countByCompany(request.getCompany());

        GradingSubmission submission = gradingMapper.toEntity(request);
        submission.setSubmissionName(String.format("%s Submission #%d", request.getCompany(), count + 1));
        gradingRepo.save(submission);

        if (!itemIds.isEmpty()) {
            itemRepo.attachToSubmission(itemIds, submission.getId());
        }

        return submission.getId();
    }

    public void update(Long id, GradingRequest request) {
        GradingSubmission submission = findById(id);
        List<Long> itemIds = request.getItemIds();

        itemRepo.detachFromSubmission(id);
        if (!itemIds.isEmpty()) {
            itemRepo.attachToSubmission(itemIds, id);
        }

        gradingMapper.updateEntity(request, submission);
    }

    public void updateStatus(Long id, GradingAction action, List<GradingItemRequest> grades) {
        GradingSubmission submission = findById(id);
        switch (action) {
            case SEND -> {
                submission.setStatus(GradingStatus.IN_GRADING);
                submission.setSendDate(LocalDate.now());
            }
            case RETURN -> recordReturn(submission, grades);
        }
    }

    public void delete(Long id) {
        itemRepo.detachFromSubmission(id);
        gradingRepo.deleteById(id);
    }

    private void recordReturn(GradingSubmission submission, List<GradingItemRequest> grades) {
        double totalUpcharge = 0;
        for (GradingItemRequest grade : grades) {
            TrackedItem item = itemRepo.findById(grade.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("TrackedItem", grade.getItemId()));
            item.setGradedDetails(gradedDetailsMapper.fromGradeRequest(grade, submission.getCompany()));
            item.setStatus(ItemStatus.AVAILABLE);
            item.setItemType(ItemType.GRADED_CARD);
            totalUpcharge += grade.getUpcharge();
        }

        submission.setUpchargeTotal(totalUpcharge);
        submission.setReturnDate(LocalDate.now());
        submission.setStatus(GradingStatus.RETURNED);
    }

    private GradingSubmission findById(Long id) {
        return gradingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GradingSubmission", id));
    }
}
