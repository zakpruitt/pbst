package com.collectingwithzak.mapper;

import com.collectingwithzak.dto.lot.LotRequest;
import com.collectingwithzak.dto.lot.LotResponse;
import com.collectingwithzak.entity.LotPurchase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {TrackedItemMapper.class})
public interface LotMapper {

    @Mapping(target = "snapshotItems", expression = "java(entity.parseSnapshot())")
    LotResponse toResponse(LotPurchase entity);

    List<LotResponse> toResponseList(List<LotPurchase> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "trackedItems", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    LotPurchase toEntity(LotRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "trackedItems", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(LotRequest request, @MappingTarget LotPurchase entity);
}
