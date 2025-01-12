package com.zakpruitt.pbst.mappers;

import com.zakpruitt.pbst.dtos.SealedProductDTO;
import com.zakpruitt.pbst.entities.SealedProduct;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = SingleCardMapper.class)
public interface SealedProductMapper {
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
    SealedProduct toEntity(SealedProductDTO dto);

    // Mapping for updates (ignores null values to prevent overwriting)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDTO(SealedProductDTO dto, @MappingTarget SealedProduct entity);
}
