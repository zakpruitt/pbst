package com.collectingwithzak.mapper;

import com.collectingwithzak.dto.inventory.SealedProductResponse;
import com.collectingwithzak.entity.SealedProduct;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SealedProductMapper {

    SealedProductResponse toResponse(SealedProduct entity);
}
