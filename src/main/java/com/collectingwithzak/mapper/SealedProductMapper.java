package com.collectingwithzak.mapper;

import com.collectingwithzak.dto.response.SealedProductResponse;
import com.collectingwithzak.dto.response.SealedSearchResult;
import com.collectingwithzak.entity.SealedProduct;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SealedProductMapper {

    SealedProductResponse toResponse(SealedProduct entity);

    SealedSearchResult toSearchResult(SealedProduct entity);

    List<SealedSearchResult> toSearchResultList(List<SealedProduct> entities);
}
