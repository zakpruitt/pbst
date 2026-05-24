package com.zakpruitt.collectingwithzak.service;

import com.zakpruitt.collectingwithzak.dto.request.GradingItemRequest;
import com.zakpruitt.collectingwithzak.dto.request.GradingRequest;
import com.zakpruitt.collectingwithzak.entity.GradingSubmission;
import com.zakpruitt.collectingwithzak.entity.TrackedItem;
import com.zakpruitt.collectingwithzak.entity.enums.GradingAction;
import com.zakpruitt.collectingwithzak.entity.enums.GradingStatus;
import com.zakpruitt.collectingwithzak.entity.enums.ItemStatus;
import com.zakpruitt.collectingwithzak.entity.enums.ItemType;
import com.zakpruitt.collectingwithzak.exception.ResourceNotFoundException;
import com.zakpruitt.collectingwithzak.mapper.GradedDetailsMapper;
import com.zakpruitt.collectingwithzak.mapper.GradingMapper;
import com.zakpruitt.collectingwithzak.repository.GradingSubmissionRepository;
import com.zakpruitt.collectingwithzak.repository.TrackedItemRepository;
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
            itemRepo.findAllById(itemIds).forEach(item -> {
                item.setGradingSubmission(submission);
                item.setStatus(ItemStatus.IN_GRADING);
            });
        }

        return submission.getId();
    }

    public void update(Long id, GradingRequest request) {
        GradingSubmission submission = findWithItemsById(id);
        submission.getItems().forEach(item -> {
            item.setGradingSubmission(null);
            item.setStatus(ItemStatus.AVAILABLE);
        });

        List<Long> itemIds = request.getItemIds();
        if (!itemIds.isEmpty()) {
            itemRepo.findAllById(itemIds).forEach(item -> {
                item.setGradingSubmission(submission);
                item.setStatus(ItemStatus.IN_GRADING);
            });
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
        GradingSubmission submission = findWithItemsById(id);
        submission.getItems().forEach(item -> {
            item.setGradingSubmission(null);
            item.setStatus(ItemStatus.AVAILABLE);
        });
        gradingRepo.delete(submission);
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

    private GradingSubmission findWithItemsById(Long id) {
        return gradingRepo.findWithItemsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GradingSubmission", id));
    }
}
