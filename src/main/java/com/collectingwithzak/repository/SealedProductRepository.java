package com.collectingwithzak.repository;

import com.collectingwithzak.entity.SealedProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SealedProductRepository extends JpaRepository<SealedProduct, String> {

    @Query("SELECT p FROM SealedProduct p " +
           "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(p.setName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY p.name LIMIT 10")
    List<SealedProduct> search(String query);
}
