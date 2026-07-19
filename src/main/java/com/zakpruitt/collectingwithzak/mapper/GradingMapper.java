package com.zakpruitt.collectingwithzak.mapper;

import com.zakpruitt.collectingwithzak.dto.request.GradingRequest;
import com.zakpruitt.collectingwithzak.entity.GradingSubmission;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface GradingMapper {

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
