package com.zakpruitt.collectingwithzak.mapper;

import com.zakpruitt.collectingwithzak.dto.request.CreateInventoryRequest;
import com.zakpruitt.collectingwithzak.dto.request.InventoryItemRow;
import com.zakpruitt.collectingwithzak.dto.request.SnapshotItem;
import com.zakpruitt.collectingwithzak.dto.request.UpdateInventoryRequest;
import com.zakpruitt.collectingwithzak.dto.response.TrackedItemResponse;
import com.zakpruitt.collectingwithzak.entity.LotPurchase;
import com.zakpruitt.collectingwithzak.entity.TrackedItem;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {PokemonCardMapper.class, SealedProductMapper.class})
public interface TrackedItemMapper {

    @Mapping(target = "gradingFee", expression = "java(entity.getGradingFee())")
    @Mapping(target = "totalCostBasis", expression = "java(entity.getTotalCostBasis())")
    @Mapping(source = "gradedDetails.gradingCompany", target = "gradingCompany")
    @Mapping(source = "gradedDetails.grade", target = "grade")
    @Mapping(source = "gradedDetails.gradingUpcharge", target = "gradingUpcharge")
    @Mapping(source = "lotPurchase.id", target = "lotPurchaseId")
    @Mapping(source = "lotPurchase.sellerName", target = "lotPurchaseSellerName")
    @Mapping(source = "gradingSubmission.id", target = "gradingSubmissionId")
    @Mapping(source = "gradingSubmission.submissionName", target = "gradingSubmissionName")
    @Mapping(source = "gradingSubmission.costPerCard", target = "gradingCostPerCard")
    TrackedItemResponse toResponse(TrackedItem entity);

    List<TrackedItemResponse> toResponseList(List<TrackedItem> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "name", target = "manualNameOverride")
    @Mapping(source = "marketValue", target = "marketValueAtPurchase")
    @Mapping(target = "itemType", ignore = true)
    @Mapping(target = "lotPurchase", ignore = true)
    @Mapping(target = "pokemonCard", ignore = true)
    @Mapping(target = "sealedProduct", ignore = true)
    @Mapping(target = "gradingSubmission", ignore = true)
    @Mapping(target = "sale", ignore = true)
    @Mapping(target = "gradedDetails", ignore = true)
    void updateEntity(UpdateInventoryRequest request, @MappingTarget TrackedItem entity);

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(source = "row.name", target = "manualNameOverride")
    @Mapping(source = "row.marketValue", target = "marketValueAtPurchase")
    @Mapping(source = "row.itemType", target = "itemType")
    @Mapping(source = "row.costBasis", target = "costBasis")
    @Mapping(source = "request.purpose", target = "purpose")
    @Mapping(source = "request.acquisitionDate", target = "acquisitionDate")
    TrackedItem fromSnapshotRow(InventoryItemRow row, CreateInventoryRequest request);

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(source = "item.name", target = "manualNameOverride")
    @Mapping(source = "item.marketPrice", target = "marketValueAtPurchase")
    @Mapping(target = "costBasis", expression = "java(item.getOffered() / item.getQty())")
    @Mapping(source = "lot.purchaseDate", target = "acquisitionDate")
    @Mapping(source = "lot", target = "lotPurchase")
    @Mapping(target = "purpose", ignore = true)
    @Mapping(target = "status", ignore = true)
    TrackedItem fromSnapshotItem(SnapshotItem item, LotPurchase lot);
}
