package com.collectingwithzak.mapper;

import com.collectingwithzak.dto.response.GradingSubmissionResponse;
import com.collectingwithzak.entity.GradingSubmission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {TrackedItemMapper.class})
public interface GradingMapper {

    @Mapping(target = "grandTotal", expression = "java(entity.getGrandTotal())")
    GradingSubmissionResponse toResponse(GradingSubmission entity);

    List<GradingSubmissionResponse> toResponseList(List<GradingSubmission> entities);
}
