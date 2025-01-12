package com.zakpruitt.pbst.mappers;

import com.zakpruitt.pbst.dtos.GradingSubmissionDTO;
import com.zakpruitt.pbst.entities.GradingSubmission;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {TrackedItemMapper.class})
public interface GradingSubmissionMapper {
    GradingSubmissionDTO toDto(GradingSubmission gradingSubmission);

    GradingSubmission toEntity(GradingSubmissionDTO gradingSubmissionDTO);
}
