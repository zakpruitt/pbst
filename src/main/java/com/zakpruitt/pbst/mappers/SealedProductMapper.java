package com.zakpruitt.pbst.mappers;

import com.zakpruitt.pbst.dtos.SealedProductDTO;
import com.zakpruitt.pbst.dtos.SealedProductUpdateDTO;
import com.zakpruitt.pbst.entities.SealedProduct;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface SealedProductMapper {
    SealedProduct toEntity(SealedProductDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(SealedProductUpdateDTO dto, @MappingTarget SealedProduct entity);
}
