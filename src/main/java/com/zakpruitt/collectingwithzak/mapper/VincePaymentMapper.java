package com.zakpruitt.collectingwithzak.mapper;

import com.zakpruitt.collectingwithzak.dto.request.CreateVincePaymentRequest;
import com.zakpruitt.collectingwithzak.entity.VincePayment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface VincePaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "description", defaultValue = "")
    VincePayment toEntity(CreateVincePaymentRequest request);
}
