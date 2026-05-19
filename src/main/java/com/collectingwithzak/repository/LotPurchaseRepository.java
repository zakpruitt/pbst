package com.collectingwithzak.repository;

import com.collectingwithzak.dto.dashboard.LabeledStat;
import com.collectingwithzak.entity.LotPurchase;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface LotPurchaseRepository extends JpaRepository<LotPurchase, Long> {
    @Query("SELECT DISTINCT l FROM LotPurchase l LEFT JOIN FETCH l.trackedItems ORDER BY l.purchaseDate DESC")
    List<LotPurchase> findAllWithItemsOrderByPurchaseDateDesc();

    @Modifying
    @Query("UPDATE LotPurchase l SET l.status = :status WHERE l.id = :id")
    void updateStatus(Long id, String status);

    List<LotPurchase> findByOrderByPurchaseDateDesc(Pageable pageable);

    @Query("SELECT COALESCE(SUM(l.totalCost), 0) FROM LotPurchase l WHERE l.status != 'REJECTED'")
    double getTotalCostNonRejected();

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('month', purchase_date), 'YYYY-MM') AS month, " +
            "COALESCE(SUM(total_cost), 0) AS spend " +
            "FROM lot_purchases WHERE status != 'REJECTED' " +
            "AND purchase_date >= NOW() - make_interval(months => :months) " +
            "GROUP BY month ORDER BY month", nativeQuery = true)
    List<Object[]> getMonthlySpendRaw(int months);

    @Query("SELECT new com.collectingwithzak.dto.dashboard.LabeledStat(l.status, COUNT(l)) FROM LotPurchase l GROUP BY l.status")
    List<LabeledStat> countByStatus();

    default Map<String, Double> getMonthlySpend(int months) {
        Map<String, Double> result = new LinkedHashMap<>();
        for (Object[] row : getMonthlySpendRaw(months)) {
            result.put((String) row[0], ((Number) row[1]).doubleValue());
        }
        return result;
    }
}
