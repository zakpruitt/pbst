package com.collectingwithzak.repository;

import com.collectingwithzak.entity.SealedProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SealedProductRepository extends JpaRepository<SealedProduct, String>, JpaSpecificationExecutor<SealedProduct> {
}
