package com.zakpruitt.collectingwithzak.repository;

import com.zakpruitt.collectingwithzak.dto.common.LabeledStat;
import com.zakpruitt.collectingwithzak.entity.LotPurchase;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LotPurchaseRepository extends JpaRepository<LotPurchase, Long> {

    @EntityGraph(attributePaths = {"trackedItems"})
    @Query("SELECT DISTINCT l FROM LotPurchase l ORDER BY l.purchaseDate DESC")
    List<LotPurchase> findAllWithItemsOrderByPurchaseDateDesc();

    List<LotPurchase> findByOrderByPurchaseDateDesc(Pageable pageable);

    @Query("SELECT COALESCE(SUM(l.totalCost), 0) FROM LotPurchase l WHERE l.status != 'REJECTED'")
    double getTotalCostNonRejected();

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('month', purchase_date), 'YYYY-MM') AS month, " +
            "COALESCE(SUM(total_cost), 0) AS spend " +
            "FROM lot_purchases WHERE status != 'REJECTED' " +
            "AND purchase_date >= NOW() - make_interval(months => :months) " +
            "GROUP BY month ORDER BY month", nativeQuery = true)
    List<Object[]> getMonthlySpendRaw(int months);

    @Query("SELECT new com.zakpruitt.collectingwithzak.dto.common.LabeledStat(l.status, COUNT(l)) " +
            "FROM LotPurchase l GROUP BY l.status")
    List<LabeledStat> countByStatus();

}
