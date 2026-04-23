package com.collectingwithzak.mapper;

import com.collectingwithzak.dto.response.*;
import com.collectingwithzak.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {PokemonCardMapper.class, SealedProductMapper.class})
public interface TrackedItemMapper {

    @Mapping(target = "gradingFee", expression = "java(entity.getGradingFee())")
    @Mapping(target = "totalCostBasis", expression = "java(entity.getTotalCostBasis())")
    TrackedItemResponse toResponse(TrackedItem entity);

    List<TrackedItemResponse> toResponseList(List<TrackedItem> entities);

    LotPurchaseSummary toLotSummary(LotPurchase entity);

    GradingSubmissionSummary toGradingSummary(GradingSubmission entity);

    GradedDetailsResponse toGradedDetailsResponse(GradedDetails entity);
}
