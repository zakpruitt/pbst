package com.zakpruitt.pbst.repositories;

import com.zakpruitt.pbst.entities.Sale;
import com.zakpruitt.pbst.enums.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    List<Sale> findByProductType(ProductType productType);

    List<Sale> findBySaleDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT SUM(s.salePrice) FROM Sale s")
    Double getTotalRevenue();

    @Query("SELECT SUM(s.profit) FROM Sale s")
    Double getTotalProfit();

    @Query("SELECT s FROM Sale s WHERE s.productType = :productType AND s.saleDate BETWEEN :startDate AND :endDate")
    List<Sale> findByProductTypeAndDateRange(ProductType productType, LocalDate startDate, LocalDate endDate);
}
