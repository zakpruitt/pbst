package com.zakpruitt.collectingwithzak.mapper;

import com.zakpruitt.collectingwithzak.dto.request.GradingItemRequest;
import com.zakpruitt.collectingwithzak.dto.request.SnapshotItem;
import com.zakpruitt.collectingwithzak.dto.request.UpdateInventoryRequest;
import com.zakpruitt.collectingwithzak.entity.GradedDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface GradedDetailsMapper {

    @Mapping(source = "request.upcharge", target = "gradingUpcharge")
    @Mapping(source = "company", target = "gradingCompany")
    GradedDetails fromGradeRequest(GradingItemRequest request, String company);

    @Mapping(target = "gradingUpcharge", constant = "0")
    GradedDetails fromSnapshotItem(SnapshotItem item);

    @Mapping(target = "gradingUpcharge", ignore = true)
    GradedDetails fromUpdateRequest(UpdateInventoryRequest request);

    @Mapping(target = "gradingUpcharge", ignore = true)
    void updateFromRequest(UpdateInventoryRequest request, @MappingTarget GradedDetails details);
}
