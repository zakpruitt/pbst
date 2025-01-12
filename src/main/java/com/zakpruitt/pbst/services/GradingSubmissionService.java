package com.zakpruitt.pbst.services;

import com.zakpruitt.pbst.dtos.GradingSubmissionDTO;
import com.zakpruitt.pbst.dtos.TrackedItemDTO;
import com.zakpruitt.pbst.entities.GradedDetails;
import com.zakpruitt.pbst.entities.GradingSubmission;
import com.zakpruitt.pbst.entities.TrackedItem;
import com.zakpruitt.pbst.enums.ItemGradingStatus;
import com.zakpruitt.pbst.enums.ItemType;
import com.zakpruitt.pbst.enums.SubmissionStatus;
import com.zakpruitt.pbst.exception.ResourceNotFoundException;
import com.zakpruitt.pbst.mappers.GradingSubmissionMapper;
import com.zakpruitt.pbst.repositories.GradingSubmissionRepository;
import com.zakpruitt.pbst.repositories.TrackedItemRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@AllArgsConstructor
public class GradingSubmissionService {

    private final GradingSubmissionRepository gradingSubmissionRepository;
    private final GradingSubmissionMapper gradingSubmissionMapper;
    private final TrackedItemRepository trackedItemRepository;

    public List<GradingSubmissionDTO> getAllSubmissions() {
        return gradingSubmissionRepository.findAll().stream()
                .map(gradingSubmissionMapper::toDto)
                .collect(Collectors.toList());
    }

    public GradingSubmissionDTO getSubmissionById(Long id) {
        return gradingSubmissionRepository.findById(id)
                .map(gradingSubmissionMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("GradingSubmission not found with id: " + id));
    }

    public GradingSubmissionDTO saveSubmission(GradingSubmissionDTO dto) {
        // Auto-generate name if not provided
        if (dto.getSubmissionName() == null || dto.getSubmissionName().isEmpty()) {
            long count = gradingSubmissionRepository.count();
            dto.setSubmissionName(dto.getCompany() + " Submission #" + (count + 1));
        }

        GradingSubmission submission = gradingSubmissionMapper.toEntity(dto);
        
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            List<Long> itemIds = dto.getItems().stream().map(TrackedItemDTO::getId).collect(Collectors.toList());
            List<TrackedItem> items = trackedItemRepository.findAllById(itemIds);
            
            for (TrackedItem item : items) {
                item.setGradingSubmission(submission);
                item.setGradingStatus(ItemGradingStatus.IN_GRADING);
            }
            submission.setItems(items);
        }

        GradingSubmission savedSubmission = gradingSubmissionRepository.save(submission);
        return gradingSubmissionMapper.toDto(savedSubmission);
    }

    public void updateStatus(Long id, SubmissionStatus status) {
        GradingSubmission submission = gradingSubmissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GradingSubmission not found with id: " + id));
        submission.setStatus(status);
        gradingSubmissionRepository.save(submission);
    }

    public void finalizeSubmission(GradingSubmissionDTO dto) {
        GradingSubmission submission = gradingSubmissionRepository.findById(dto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("GradingSubmission not found with id: " + dto.getId()));

        BigDecimal totalUpcharges = BigDecimal.ZERO;

        if (dto.getItems() != null) {
            for (TrackedItemDTO itemDto : dto.getItems()) {
                TrackedItem item = trackedItemRepository.findById(itemDto.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + itemDto.getId()));
                
                // Update Grading Details
                if (item.getGradedDetails() == null) {
                    item.setGradedDetails(new GradedDetails());
                }
                item.getGradedDetails().setGradingCompany(submission.getCompany());
                item.getGradedDetails().setGrade(itemDto.getGrade());

                // Update Upcharge
                if (itemDto.getGradingUpcharge() != null) {
                    item.getGradedDetails().setGradingUpcharge(itemDto.getGradingUpcharge());
                    totalUpcharges = totalUpcharges.add(itemDto.getGradingUpcharge());
                }

                // Update Status & Type
                item.setGradingStatus(ItemGradingStatus.GRADED);
                item.setItemType(ItemType.GRADED_CARD);
                item.setPurpose(itemDto.getPurpose()); // Update purpose (Inventory vs Collection)
                
                trackedItemRepository.save(item);
            }
        }

        // Add upcharges to total cost
        if (submission.getTotalGradingCost() != null) {
            submission.setTotalGradingCost(submission.getTotalGradingCost().add(totalUpcharges));
        } else {
            submission.setTotalGradingCost(totalUpcharges);
        }

        submission.setStatus(SubmissionStatus.RETURNED);
        submission.setReturnDate(java.time.LocalDate.now());
        gradingSubmissionRepository.save(submission);
    }

    public void deleteSubmission(Long id) {
        if (!gradingSubmissionRepository.existsById(id)) {
            throw new ResourceNotFoundException("GradingSubmission not found with id: " + id);
        }
        List<TrackedItem> items = trackedItemRepository.findByGradingSubmissionId(id);
        for (TrackedItem item : items) {
            item.setGradingSubmission(null);
            item.setGradingStatus(ItemGradingStatus.TO_GRADE);
        }
        gradingSubmissionRepository.deleteById(id);
    }

    public BigDecimal getTotalGradingCost() {
        return gradingSubmissionRepository.sumTotalGradingCost();
    }

    public long countActiveSubmissions() {
        return gradingSubmissionRepository.findAll().stream()
                .filter(s -> s.getStatus() != SubmissionStatus.RETURNED)
                .count();
    }
}
