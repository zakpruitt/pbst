package com.zakpruitt.pbst.mappers;

import com.zakpruitt.pbst.dtos.SaleDTO;
import com.zakpruitt.pbst.entities.Sale;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SaleMapper {
    SaleDTO toDto(Sale sale);

    @Mapping(target = "id", ignore = true) // ID is auto-generated
    Sale toEntity(SaleDTO saleDTO);
}
