package com.zakpruitt.collectingwithzak.service.render;

import com.zakpruitt.collectingwithzak.dto.response.GradingSubmissionResponse;
import com.zakpruitt.collectingwithzak.dto.response.TrackedItemResponse;
import com.zakpruitt.collectingwithzak.entity.GradingSubmission;
import com.zakpruitt.collectingwithzak.entity.TrackedItem;
import com.zakpruitt.collectingwithzak.entity.enums.ItemStatus;
import com.zakpruitt.collectingwithzak.exception.ResourceNotFoundException;
import com.zakpruitt.collectingwithzak.mapper.GradingMapper;
import com.zakpruitt.collectingwithzak.mapper.TrackedItemMapper;
import com.zakpruitt.collectingwithzak.repository.GradingSubmissionRepository;
import com.zakpruitt.collectingwithzak.repository.TrackedItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradingRenderService {

    private final GradingSubmissionRepository gradingRepo;
    private final TrackedItemRepository itemRepo;
    private final GradingMapper gradingMapper;
    private final TrackedItemMapper trackedItemMapper;

    public List<GradingSubmissionResponse> getAll() {
        List<GradingSubmission> submissions = gradingRepo.findAllByOrderByCreatedAtDesc();
        return gradingMapper.toResponseList(submissions);
    }

    public GradingSubmissionResponse getByIdWithItems(Long id) {
        GradingSubmission submission = findByIdWithItems(id);
        return gradingMapper.toResponse(submission);
    }

    public List<TrackedItemResponse> getInventoryItems() {
        List<TrackedItem> items = itemRepo.findByStatusAndSaleIsNull(ItemStatus.AVAILABLE);
        return trackedItemMapper.toResponseList(items);
    }

    public List<TrackedItemResponse> getAvailableItemsForSubmission(Long submissionId) {
        GradingSubmission submission = findByIdWithItems(submissionId);

        List<TrackedItem> allItems = new ArrayList<>(itemRepo.findByStatusAndSaleIsNull(ItemStatus.AVAILABLE));
        allItems.addAll(submission.getItems());
        return trackedItemMapper.toResponseList(allItems);
    }

    private GradingSubmission findByIdWithItems(Long id) {
        return gradingRepo.findWithItemsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GradingSubmission", id));
    }
}
