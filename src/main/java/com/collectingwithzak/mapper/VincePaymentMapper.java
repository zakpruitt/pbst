package com.collectingwithzak.mapper;

import com.collectingwithzak.dto.request.CreateVincePaymentRequest;
import com.collectingwithzak.dto.response.VincePaymentResponse;
import com.collectingwithzak.entity.VincePayment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface VincePaymentMapper {

    VincePaymentResponse toResponse(VincePayment entity);

    List<VincePaymentResponse> toResponseList(List<VincePayment> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    VincePayment toEntity(CreateVincePaymentRequest request);
}
