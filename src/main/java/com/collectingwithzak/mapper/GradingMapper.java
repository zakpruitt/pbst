package com.collectingwithzak.mapper;

import com.collectingwithzak.dto.request.GradingRequest;
import com.collectingwithzak.dto.response.GradingSubmissionResponse;
import com.collectingwithzak.entity.GradingSubmission;
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
    @Mapping(target = "costPerCard", expression = "java(request.getItemIds().isEmpty() ? 0 : request.getSubmissionCost() / request.getItemIds().size())")
    GradingSubmission toEntity(GradingRequest request);

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "costPerCard", expression = "java(request.getItemIds().isEmpty() ? 0 : request.getSubmissionCost() / request.getItemIds().size())")
    void updateEntity(GradingRequest request, @MappingTarget GradingSubmission entity);
}
