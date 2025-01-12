package com.zakpruitt.pbst.repositories;

import com.zakpruitt.pbst.entities.SealedProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SealedProductRepository extends JpaRepository<SealedProduct, Long> {
    List<SealedProduct> findByQuantityGreaterThanQuantityRipped();

    List<SealedProduct> findByPurchaseDate(LocalDate purchaseDate);
}

