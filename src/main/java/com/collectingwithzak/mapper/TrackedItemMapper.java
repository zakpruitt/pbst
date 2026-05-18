package com.collectingwithzak.mapper;

import com.collectingwithzak.dto.InventorySnapshotRow;
import com.collectingwithzak.dto.SnapshotItem;
import com.collectingwithzak.dto.request.UpdateInventoryRequest;
import com.collectingwithzak.dto.response.*;
import com.collectingwithzak.entity.*;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {PokemonCardMapper.class, SealedProductMapper.class, GradedDetailsMapper.class})
public interface TrackedItemMapper {

    @Mapping(target = "gradingFee", expression = "java(entity.getGradingFee())")
    @Mapping(target = "totalCostBasis", expression = "java(entity.getTotalCostBasis())")
    TrackedItemResponse toResponse(TrackedItem entity);

    List<TrackedItemResponse> toResponseList(List<TrackedItem> entities);

    LotPurchaseSummary toLotSummary(LotPurchase entity);

    GradingSubmissionSummary toGradingSummary(GradingSubmission entity);

    GradedDetailsResponse toGradedDetailsResponse(GradedDetails entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "name", target = "manualNameOverride")
    @Mapping(source = "marketValue", target = "marketValueAtPurchase")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "itemType", ignore = true)
    @Mapping(target = "lotPurchase", ignore = true)
    @Mapping(target = "pokemonCard", ignore = true)
    @Mapping(target = "sealedProduct", ignore = true)
    @Mapping(target = "gradingSubmission", ignore = true)
    @Mapping(target = "sale", ignore = true)
    @Mapping(target = "gradedDetails", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateInventoryRequest request, @MappingTarget TrackedItem entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "name", target = "manualNameOverride")
    @Mapping(source = "marketValue", target = "marketValueAtPurchase")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "purpose", ignore = true)
    @Mapping(target = "acquisitionDate", ignore = true)
    @Mapping(target = "notes", ignore = true)
    @Mapping(target = "lotPurchase", ignore = true)
    @Mapping(target = "pokemonCard", ignore = true)
    @Mapping(target = "sealedProduct", ignore = true)
    @Mapping(target = "gradingSubmission", ignore = true)
    @Mapping(target = "sale", ignore = true)
    @Mapping(target = "gradedDetails", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TrackedItem fromSnapshotRow(InventorySnapshotRow row);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "name", target = "manualNameOverride")
    @Mapping(source = "marketPrice", target = "marketValueAtPurchase")
    @Mapping(target = "costBasis", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "acquisitionDate", ignore = true)
    @Mapping(target = "notes", ignore = true)
    @Mapping(target = "lotPurchase", ignore = true)
    @Mapping(target = "pokemonCard", ignore = true)
    @Mapping(target = "sealedProduct", ignore = true)
    @Mapping(target = "gradingSubmission", ignore = true)
    @Mapping(target = "sale", ignore = true)
    @Mapping(target = "gradedDetails", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TrackedItem fromSnapshotItem(SnapshotItem item);
}
