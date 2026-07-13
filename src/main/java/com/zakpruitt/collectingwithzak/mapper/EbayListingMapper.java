package com.zakpruitt.collectingwithzak.mapper;

import com.zakpruitt.collectingwithzak.dto.response.EbayListingResponse;
import com.zakpruitt.collectingwithzak.entity.EbayListing;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EbayListingMapper {

    @Mapping(source = "lotPurchase.id", target = "lotPurchaseId")
    EbayListingResponse toResponse(EbayListing entity);

    List<EbayListingResponse> toResponseList(List<EbayListing> entities);
}
