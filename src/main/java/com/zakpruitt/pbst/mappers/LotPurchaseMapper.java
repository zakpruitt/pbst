package com.zakpruitt.pbst.mappers;

import com.zakpruitt.pbst.dtos.LotPurchaseDTO;
import com.zakpruitt.pbst.entities.LotPurchase;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {TrackedItemMapper.class})
public interface LotPurchaseMapper {
    LotPurchaseDTO toDto(LotPurchase lotPurchase);

    LotPurchase toEntity(LotPurchaseDTO lotPurchaseDTO);
}