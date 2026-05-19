package com.collectingwithzak.mapper;

import com.collectingwithzak.dto.grading.GradingRequest;
import com.collectingwithzak.dto.grading.GradingSubmissionResponse;
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
    GradingSubmission toEntity(GradingRequest request);

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    void updateEntity(GradingRequest request, @MappingTarget GradingSubmission entity);
}
