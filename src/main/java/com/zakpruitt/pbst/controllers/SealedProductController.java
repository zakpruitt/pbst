package com.zakpruitt.pbst.controllers;

import com.zakpruitt.pbst.dtos.SealedProductDTO;
import com.zakpruitt.pbst.entities.SealedProduct;
import com.zakpruitt.pbst.mappers.SealedProductMapper;
import com.zakpruitt.pbst.services.SealedProductService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/sealed-products")
@Validated
@AllArgsConstructor
public class SealedProductController {

    private final SealedProductService sealedProductService;

    @GetMapping
    public ResponseEntity<List<SealedProduct>> getAllSealedProducts() {
        List<SealedProduct> products = sealedProductService.getAllSealedProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SealedProduct> getSealedProductById(@PathVariable Long id) {
        Optional<SealedProduct> product = sealedProductService.getSealedProductById(id);
        return product.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<SealedProduct> saveSealedProduct(@Valid @RequestBody SealedProductDTO sealedProductDto) {
        SealedProduct newSealedProduct = sealedProductService.saveSealedProduct(sealedProductDto);
        return ResponseEntity.ok(newSealedProduct);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SealedProduct> updateSealedProduct(
            @PathVariable Long id,
            @Valid @RequestBody SealedProductDTO sealedProductDto) {
        SealedProduct editedSealedProduct = sealedProductService.editSealedProduct(id, sealedProductDto);
        return ResponseEntity.ok(editedSealedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSealedProduct(@PathVariable Long id) {
        sealedProductService.deleteSealedProduct(id);
        return ResponseEntity.noContent().build();
    }
}
