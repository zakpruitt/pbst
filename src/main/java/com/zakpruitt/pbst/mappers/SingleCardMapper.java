package com.zakpruitt.pbst.mappers;

import com.zakpruitt.pbst.dtos.SingleCardDTO;
import com.zakpruitt.pbst.entities.SingleCard;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SingleCardMapper {
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
    @Mapping(target = "submittedForGrading", constant = "false")
    @Mapping(target = "gradingCost", constant = "0.0")
    SingleCard toEntity(SingleCardDTO dto);

    @Mapping(source = "sealedProduct.id", target = "sealedProductId")
    SingleCardDTO toDTO(SingleCard singleCard);
}
