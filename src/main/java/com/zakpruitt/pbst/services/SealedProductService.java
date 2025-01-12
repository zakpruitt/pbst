package com.zakpruitt.pbst.services;

import com.zakpruitt.pbst.dtos.SealedProductDTO;
import com.zakpruitt.pbst.dtos.SealedProductUpdateDTO;
import com.zakpruitt.pbst.entities.SealedProduct;
import com.zakpruitt.pbst.exception.ResourceNotFoundException;
import com.zakpruitt.pbst.mappers.SealedProductMapper;
import com.zakpruitt.pbst.repositories.SealedProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@AllArgsConstructor
public class SealedProductService {

    private final SealedProductMapper sealedProductMapper;
    private final SealedProductRepository sealedProductRepository;

    public List<SealedProduct> getAllSealedProducts() {
        return sealedProductRepository.findAll();
    }

    public Optional<SealedProduct> getSealedProductById(Long id) {
        return sealedProductRepository.findById(id);
    }

    public SealedProduct saveSealedProduct(SealedProductDTO sealedProductDto) {
        SealedProduct newSealedProduct = sealedProductMapper.toEntity(sealedProductDto);
        return sealedProductRepository.save(newSealedProduct);
    }

    public SealedProduct editSealedProduct(Long id, SealedProductUpdateDTO sealedProductDto) {
        Optional<SealedProduct> existingSealedProductOptional = sealedProductRepository.findById(id);
        if (existingSealedProductOptional.isEmpty()) {
            throw new ResourceNotFoundException("SealedProduct with the ID %d could not be found during an edit call."
                    .formatted(id)
            );
        }

        SealedProduct existingSealedProduct = existingSealedProductOptional.get();
        sealedProductMapper.updateEntityFromDTO(sealedProductDto, existingSealedProduct);
        return sealedProductRepository.save(existingSealedProduct);
    }

    public void deleteSealedProduct(Long id) {
        sealedProductRepository.deleteById(id);
    }
}
