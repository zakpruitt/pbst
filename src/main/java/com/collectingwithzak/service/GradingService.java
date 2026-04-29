package com.collectingwithzak.service;

import com.collectingwithzak.dto.request.CreateGradingRequest;
import com.collectingwithzak.dto.request.ItemGradeRequest;
import com.collectingwithzak.dto.request.UpdateGradingRequest;
import com.collectingwithzak.dto.response.GradingFormData;
import com.collectingwithzak.dto.response.GradingSubmissionResponse;
import com.collectingwithzak.dto.response.TrackedItemResponse;
import com.collectingwithzak.entity.GradedDetails;
import com.collectingwithzak.entity.GradingSubmission;
import com.collectingwithzak.entity.TrackedItem;
import com.collectingwithzak.entity.enums.GradingStatus;
import com.collectingwithzak.entity.enums.ItemType;
import com.collectingwithzak.entity.enums.Purpose;
import com.collectingwithzak.exception.ResourceNotFoundException;
import com.collectingwithzak.mapper.GradingMapper;
import com.collectingwithzak.mapper.TrackedItemMapper;
import com.collectingwithzak.repository.GradingSubmissionRepository;
import com.collectingwithzak.repository.TrackedItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GradingService {

    private final GradingSubmissionRepository gradingRepo;
    private final TrackedItemRepository itemRepo;
    private final GradingMapper gradingMapper;
    private final TrackedItemMapper trackedItemMapper;

    public Long createWithItems(CreateGradingRequest request) {
        List<Long> itemIds = request.getItemIds() != null ? request.getItemIds() : List.of();
        long count = gradingRepo.countByCompany(request.getCompany());
        double costPerCard = itemIds.isEmpty() ? 0 : request.getSubmissionCost() / itemIds.size();

        GradingSubmission submission = new GradingSubmission();
        submission.setSubmissionName(String.format("%s Submission #%d", request.getCompany(), count + 1));
        submission.setCompany(request.getCompany());
        submission.setSubmissionMethod(request.getSubmissionMethod());
        submission.setStatus(GradingStatus.PREPPING.name());
        submission.setCostPerCard(costPerCard);
        submission.setSubmissionCost(request.getSubmissionCost());
        submission.setNotes(request.getNotes());

        submission = gradingRepo.save(submission);

        if (!itemIds.isEmpty()) {
            itemRepo.attachToSubmission(itemIds, submission.getId());
            itemRepo.updatePurposeBySubmission(submission.getId(), Purpose.IN_GRADING.name());
        }

        return submission.getId();
    }

    @Transactional(readOnly = true)
    public List<GradingSubmissionResponse> getAll() {
        return gradingMapper.toResponseList(gradingRepo.findAllWithItemsOrderByCreatedAtDesc());
    }

    @Transactional(readOnly = true)
    public GradingSubmissionResponse getByIdWithItems(Long id) {
        GradingSubmission submission = gradingRepo.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("GradingSubmission", id));
        return gradingMapper.toResponse(submission);
    }

    @Transactional(readOnly = true)
    public GradingFormData getNewFormData() {
        List<TrackedItemResponse> all = trackedItemMapper.toResponseList(
                itemRepo.findByPurpose(Purpose.INVENTORY.name()));

        return new GradingFormData(null,
                TrackedItemResponse.filterByType(all, ItemType.RAW_CARD),
                TrackedItemResponse.filterByType(all, ItemType.GRADED_CARD),
                Set.of());
    }

    @Transactional(readOnly = true)
    public GradingFormData getEditFormData(Long id) {
        GradingSubmission submission = gradingRepo.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("GradingSubmission", id));

        Set<Long> attachedIds = submission.getItems().stream()
                .map(TrackedItem::getId)
                .collect(Collectors.toSet());

        List<TrackedItem> allItems = new ArrayList<>(itemRepo.findByPurpose(Purpose.INVENTORY.name()));
        allItems.addAll(submission.getItems());

        List<TrackedItemResponse> all = trackedItemMapper.toResponseList(allItems);
        return new GradingFormData(
                gradingMapper.toResponse(submission),
                TrackedItemResponse.filterByType(all, ItemType.RAW_CARD),
                TrackedItemResponse.filterByType(all, ItemType.GRADED_CARD),
                attachedIds);
    }

    public void update(UpdateGradingRequest request, Long id) {
        GradingSubmission submission = findById(id);
        List<Long> itemIds = request.getItemIds() != null ? request.getItemIds() : List.of();

        itemRepo.detachFromSubmission(id);

        if (!itemIds.isEmpty()) {
            itemRepo.attachToSubmission(itemIds, id);
            itemRepo.updatePurposeBySubmission(id, Purpose.IN_GRADING.name());
        }

        double costPerCard = itemIds.isEmpty() ? 0 : request.getSubmissionCost() / itemIds.size();
        submission.setCompany(request.getCompany());
        submission.setSubmissionMethod(request.getSubmissionMethod());
        submission.setSubmissionCost(request.getSubmissionCost());
        submission.setCostPerCard(costPerCard);
        submission.setNotes(request.getNotes());

        gradingRepo.save(submission);
    }

    public void delete(Long id) {
        itemRepo.detachFromSubmission(id);
        gradingRepo.deleteById(id);
    }

    public void advanceStatus(Long id, String newStatus) {
        gradingRepo.updateStatus(id, newStatus);
        if (GradingStatus.IN_GRADING.name().equals(newStatus)) {
            gradingRepo.setSendDate(id, LocalDate.now());
        }
    }

    public void recordReturn(Long submissionId, List<ItemGradeRequest> grades) {
        GradingSubmission submission = findById(submissionId);

        double totalUpcharge = 0;
        for (ItemGradeRequest g : grades) {
            GradedDetails details = new GradedDetails(submission.getCompany(), g.getGrade(), g.getUpcharge());
            var item = itemRepo.findById(g.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("TrackedItem", g.getItemId()));
            item.setGradedDetails(details);
            item.setPurpose(Purpose.INVENTORY.name());
            item.setItemType(ItemType.GRADED_CARD.name());
            itemRepo.save(item);
            totalUpcharge += g.getUpcharge();
        }

        gradingRepo.updateReturnDetails(submissionId, totalUpcharge, LocalDate.now());
        gradingRepo.updateStatus(submissionId, GradingStatus.RETURNED.name());
    }

    private GradingSubmission findById(Long id) {
        return gradingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GradingSubmission", id));
    }

}
