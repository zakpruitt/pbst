package com.zakpruitt.pbst.repositories;

import com.zakpruitt.pbst.entities.LotPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface LotPurchaseRepository extends JpaRepository<LotPurchase, Long> {

    @Query("SELECT SUM(l.totalCost) FROM LotPurchase l")
    BigDecimal sumTotalCost();

}