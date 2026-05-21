package com.collectingwithzak.service.render;

import com.collectingwithzak.dto.response.GradingSubmissionResponse;
import com.collectingwithzak.dto.response.TrackedItemResponse;
import com.collectingwithzak.entity.GradingSubmission;
import com.collectingwithzak.entity.TrackedItem;
import com.collectingwithzak.exception.ResourceNotFoundException;
import com.collectingwithzak.mapper.GradingMapper;
import com.collectingwithzak.mapper.TrackedItemMapper;
import com.collectingwithzak.repository.GradingSubmissionRepository;
import com.collectingwithzak.repository.TrackedItemRepository;
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

    private GradingSubmission findByIdWithItems(Long id) {
        return gradingRepo.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("GradingSubmission", id));
    }
}
