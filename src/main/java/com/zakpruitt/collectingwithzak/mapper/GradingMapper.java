package com.zakpruitt.collectingwithzak.mapper;

import com.zakpruitt.collectingwithzak.dto.request.GradingRequest;
import com.zakpruitt.collectingwithzak.dto.response.GradingSubmissionResponse;
import com.zakpruitt.collectingwithzak.entity.GradingSubmission;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {TrackedItemMapper.class})
public interface GradingMapper {

    @Mapping(target = "grandTotal", expression = "java(entity.getGrandTotal())")
    GradingSubmissionResponse toResponse(GradingSubmission entity);

    List<GradingSubmissionResponse> toResponseList(List<GradingSubmission> entities);

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "status", constant = "PREPPING")
    @Mapping(target = "costPerCard", expression = "java(costPerCard(request))")
    GradingSubmission toEntity(GradingRequest request);

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "costPerCard", expression = "java(costPerCard(request))")
    void updateEntity(GradingRequest request, @MappingTarget GradingSubmission entity);

    default double costPerCard(GradingRequest request) {
        if (request.getItemIds().isEmpty() || request.getSubmissionCost() == null) {
            return 0;
        }
        return request.getSubmissionCost() / request.getItemIds().size();
    }
}
