package com.collectingwithzak.mapper;

import com.collectingwithzak.dto.SnapshotItem;
import com.collectingwithzak.dto.request.ItemGradeRequest;
import com.collectingwithzak.dto.request.UpdateInventoryRequest;
import com.collectingwithzak.entity.GradedDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface GradedDetailsMapper {

    @Mapping(source = "upcharge", target = "gradingUpcharge")
    @Mapping(target = "gradingCompany", ignore = true)
    GradedDetails fromGradeRequest(ItemGradeRequest request);

    @Mapping(target = "gradingUpcharge", constant = "0")
    GradedDetails fromSnapshotItem(SnapshotItem item);

    @Mapping(target = "gradingUpcharge", ignore = true)
    GradedDetails fromUpdateRequest(UpdateInventoryRequest request);
}
