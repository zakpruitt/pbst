package com.collectingwithzak.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradingFormData {
    private GradingSubmissionResponse submission;
    private List<TrackedItemResponse> rawItems;
    private List<TrackedItemResponse> gradedItems;
    private Set<Long> attachedIds;
}
