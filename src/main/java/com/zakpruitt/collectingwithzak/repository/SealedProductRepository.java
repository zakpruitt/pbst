package com.zakpruitt.collectingwithzak.repository;

import com.zakpruitt.collectingwithzak.entity.SealedProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SealedProductRepository extends JpaRepository<SealedProduct, String>, JpaSpecificationExecutor<SealedProduct> {
}
