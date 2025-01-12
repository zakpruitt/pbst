package com.zakpruitt.pbst.mappers;

import com.zakpruitt.pbst.dtos.TrackedItemDTO;
import com.zakpruitt.pbst.entities.TrackedItem;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TrackedItemMapper {
    @Mapping(source = "lotPurchase.id", target = "lotPurchaseId")
    @Mapping(source = "pokemonCard.id", target = "pokemonCardId")
    @Mapping(source = "gradingSubmission.id", target = "gradingSubmissionId")
    @Mapping(source = "sale.id", target = "saleId")
    @Mapping(source = "gradedDetails.gradingCompany", target = "gradingCompany")
    @Mapping(source = "gradedDetails.grade", target = "grade")
    @Mapping(source = "gradedDetails.gradingUpcharge", target = "gradingUpcharge")
    @Mapping(source = "marketValueAtPurchase", target = "marketValueAtPurchase")
    @Mapping(source = "pokemonCard.marketPrice", target = "marketPrice")
    @Mapping(source = "manualNameOverride", target = "name")
    @Mapping(source = "pokemonCard.imageUrl", target = "imageUrl")
    @Mapping(source = "pokemonCard.setName", target = "setName")
    TrackedItemDTO toDto(TrackedItem trackedItem);

    @Mapping(source = "lotPurchaseId", target = "lotPurchase.id")
    @Mapping(source = "pokemonCardId", target = "pokemonCard.id")
    @Mapping(source = "gradingSubmissionId", target = "gradingSubmission.id")
    @Mapping(source = "saleId", target = "sale.id")
    @Mapping(source = "gradingCompany", target = "gradedDetails.gradingCompany")
    @Mapping(source = "grade", target = "gradedDetails.grade")
    @Mapping(source = "gradingUpcharge", target = "gradedDetails.gradingUpcharge")
    @Mapping(source = "marketValueAtPurchase", target = "marketValueAtPurchase")
    @Mapping(source = "name", target = "manualNameOverride")
    @Mapping(target = "id", ignore = true)
    TrackedItem toEntity(TrackedItemDTO trackedItemDTO);
}
