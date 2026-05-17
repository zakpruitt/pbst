package com.collectingwithzak.mapper;

import com.collectingwithzak.dto.request.UpdateGradingRequest;
import com.collectingwithzak.dto.response.GradingSubmissionResponse;
import com.collectingwithzak.entity.GradingSubmission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {TrackedItemMapper.class})
public interface GradingMapper {

    @Mapping(target = "grandTotal", expression = "java(entity.getGrandTotal())")
    GradingSubmissionResponse toResponse(GradingSubmission entity);

    List<GradingSubmissionResponse> toResponseList(List<GradingSubmission> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "submissionName", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "costPerCard", ignore = true)
    @Mapping(target = "sendDate", ignore = true)
    @Mapping(target = "returnDate", ignore = true)
    @Mapping(target = "upchargeTotal", ignore = true)
    @Mapping(target = "taxRate", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateGradingRequest request, @MappingTarget GradingSubmission entity);
}
