package com.collectingwithzak.mapper;

import com.collectingwithzak.dto.grading.GradingItemRequest;
import com.collectingwithzak.dto.inventory.UpdateInventoryRequest;
import com.collectingwithzak.dto.lot.SnapshotItem;
import com.collectingwithzak.entity.GradedDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface GradedDetailsMapper {

    @Mapping(source = "request.upcharge", target = "gradingUpcharge")
    @Mapping(source = "company", target = "gradingCompany")
    GradedDetails fromGradeRequest(GradingItemRequest request, String company);

    @Mapping(target = "gradingUpcharge", constant = "0")
    GradedDetails fromSnapshotItem(SnapshotItem item);

    @Mapping(target = "gradingUpcharge", ignore = true)
    GradedDetails fromUpdateRequest(UpdateInventoryRequest request);
}
