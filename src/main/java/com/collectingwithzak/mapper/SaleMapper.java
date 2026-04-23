package com.collectingwithzak.mapper;

import com.collectingwithzak.dto.ebay.EbayOrderData;
import com.collectingwithzak.dto.request.CreateSaleRequest;
import com.collectingwithzak.dto.response.SaleResponse;
import com.collectingwithzak.entity.Sale;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {TrackedItemMapper.class})
public interface SaleMapper {

    SaleResponse toResponse(Sale entity);

    List<SaleResponse> toResponseList(List<Sale> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "netAmount", expression = "java(request.getGrossAmount() - request.getEbayFees() - request.getShippingCost())")
    @Mapping(target = "orderStatus", constant = "COMPLETED")
    @Mapping(target = "status", constant = "CONFIRMED")
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "attributedTo", ignore = true)
    @Mapping(target = "notes", ignore = true)
    Sale toEntity(CreateSaleRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "netAmount", expression = "java(data.getGrossAmount() - data.getEbayFees() - data.getShippingCost())")
    @Mapping(target = "origin", constant = "EBAY")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "attributedTo", ignore = true)
    @Mapping(target = "notes", ignore = true)
    Sale fromEbayOrder(EbayOrderData data);
}
