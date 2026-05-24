package com.zakpruitt.collectingwithzak.mapper;

import com.zakpruitt.collectingwithzak.dto.response.SealedProductResponse;
import com.zakpruitt.collectingwithzak.entity.SealedProduct;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SealedProductMapper {

    SealedProductResponse toResponse(SealedProduct entity);

    List<SealedProductResponse> toResponseList(List<SealedProduct> entities);
}
