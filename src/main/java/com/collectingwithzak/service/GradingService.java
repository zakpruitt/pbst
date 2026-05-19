package com.collectingwithzak.service;

import com.collectingwithzak.dto.grading.GradingItemRequest;
import com.collectingwithzak.dto.grading.GradingRequest;
import com.collectingwithzak.dto.grading.GradingSubmissionResponse;
import com.collectingwithzak.dto.inventory.TrackedItemResponse;
import com.collectingwithzak.entity.GradedDetails;
import com.collectingwithzak.entity.GradingSubmission;
import com.collectingwithzak.entity.TrackedItem;
import com.collectingwithzak.entity.enums.GradingAction;
import com.collectingwithzak.entity.enums.GradingStatus;
import com.collectingwithzak.entity.enums.ItemStatus;
import com.collectingwithzak.entity.enums.ItemType;
import com.collectingwithzak.exception.ResourceNotFoundException;
import com.collectingwithzak.mapper.GradedDetailsMapper;
import com.collectingwithzak.mapper.GradingMapper;
import com.collectingwithzak.mapper.TrackedItemMapper;
import com.collectingwithzak.repository.GradingSubmissionRepository;
import com.collectingwithzak.repository.TrackedItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GradingService {

    private final GradingSubmissionRepository gradingRepo;
    private final TrackedItemRepository itemRepo;
    private final GradingMapper gradingMapper;
    private final GradedDetailsMapper gradedDetailsMapper;
    private final TrackedItemMapper trackedItemMapper;

    public Long createWithItems(GradingRequest request) {
        List<Long> itemIds = request.getItemIds();
        long count = gradingRepo.countByCompany(request.getCompany());

        GradingSubmission submission = gradingMapper.toEntity(request);
        submission.setSubmissionName(String.format("%s Submission #%d", request.getCompany(), count + 1));
        submission.setStatus(GradingStatus.PREPPING.name());
        submission.setCostPerCard(itemIds.isEmpty() ? 0 : request.getSubmissionCost() / itemIds.size());

        submission = gradingRepo.save(submission);

        if (!itemIds.isEmpty()) {
            itemRepo.attachToSubmission(itemIds, submission.getId());
            itemRepo.updateStatusBySubmission(submission.getId(), ItemStatus.IN_GRADING.name());
        }

        return submission.getId();
    }

    public List<GradingSubmissionResponse> getAll() {
        List<GradingSubmission> submissions = gradingRepo.findAllWithItems();
        return gradingMapper.toResponseList(submissions);
    }

    public GradingSubmissionResponse getByIdWithItems(Long id) {
        GradingSubmission submission = findByIdWithItems(id);
        return gradingMapper.toResponse(submission);
    }

    public List<TrackedItemResponse> getInventoryItems() {
        List<TrackedItem> items = itemRepo.findAvailableInventory();
        return trackedItemMapper.toResponseList(items);
    }

    public List<TrackedItemResponse> getAvailableItemsForSubmission(Long submissionId) {
        GradingSubmission submission = findByIdWithItems(submissionId);

        List<TrackedItem> allItems = new ArrayList<>(itemRepo.findAvailableInventory());
        allItems.addAll(submission.getItems());
        return trackedItemMapper.toResponseList(allItems);
    }

    public void update(Long id, GradingRequest request) {
        GradingSubmission submission = findByIdWithItems(id);
        List<Long> itemIds = request.getItemIds();

        for (TrackedItem item : submission.getItems()) {
            item.setGradingSubmission(null);
            item.setStatus(ItemStatus.AVAILABLE.name());
        }

        if (!itemIds.isEmpty()) {
            List<TrackedItem> newItems = itemRepo.findAllById(itemIds);
            for (TrackedItem item : newItems) {
                item.setGradingSubmission(submission);
                item.setStatus(ItemStatus.IN_GRADING.name());
            }
        }

        gradingMapper.updateEntity(request, submission);
        submission.setCostPerCard(itemIds.isEmpty() ? 0 : request.getSubmissionCost() / itemIds.size());
    }

    public void updateStatus(Long id, GradingAction action, List<GradingItemRequest> grades) {
        switch (action) {
            case SEND -> {
                gradingRepo.updateStatus(id, GradingStatus.IN_GRADING.name());
                gradingRepo.setSendDate(id, LocalDate.now());
            }
            case RETURN -> recordReturn(id, grades);
        }
    }

    public void delete(Long id) {
        itemRepo.detachFromSubmission(id);
        gradingRepo.deleteById(id);
    }

    private void recordReturn(Long submissionId, List<GradingItemRequest> grades) {
        GradingSubmission submission = findById(submissionId);

        double totalUpcharge = 0;
        for (GradingItemRequest grade : grades) {
            GradedDetails details = gradedDetailsMapper.fromGradeRequest(grade, submission.getCompany());

            TrackedItem item = itemRepo.findById(grade.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("TrackedItem", grade.getItemId()));
            item.setGradedDetails(details);
            item.setStatus(ItemStatus.AVAILABLE.name());
            item.setItemType(ItemType.GRADED_CARD.name());
            itemRepo.save(item);
            totalUpcharge += grade.getUpcharge();
        }

        gradingRepo.updateReturnDetails(submissionId, totalUpcharge, LocalDate.now());
        gradingRepo.updateStatus(submissionId, GradingStatus.RETURNED.name());
    }

    private GradingSubmission findById(Long id) {
        return gradingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GradingSubmission", id));
    }

    private GradingSubmission findByIdWithItems(Long id) {
        return gradingRepo.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("GradingSubmission", id));
    }
}
