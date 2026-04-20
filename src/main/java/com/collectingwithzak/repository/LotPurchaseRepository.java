package com.collectingwithzak.repository;

import com.collectingwithzak.dto.response.LotStatusCount;
import com.collectingwithzak.dto.response.MonthlySpend;
import com.collectingwithzak.entity.LotPurchase;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LotPurchaseRepository extends JpaRepository<LotPurchase, Long> {

    List<LotPurchase> findAllByOrderByPurchaseDateDesc();

    List<LotPurchase> findByOrderByPurchaseDateDesc(Pageable pageable);

    @Query("SELECT COALESCE(SUM(l.totalCost), 0) FROM LotPurchase l WHERE l.status != 'REJECTED'")
    double getTotalCostNonRejected();

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('month', purchase_date), 'YYYY-MM') AS month, " +
                   "COALESCE(SUM(total_cost), 0) AS spend, COUNT(*) AS count " +
                   "FROM lot_purchases WHERE status != 'REJECTED' " +
                   "AND purchase_date >= NOW() - make_interval(months => :months) " +
                   "GROUP BY month ORDER BY month", nativeQuery = true)
    List<Object[]> getMonthlySpendRaw(int months);

    @Query(value = "SELECT status, COUNT(*) FROM lot_purchases GROUP BY status", nativeQuery = true)
    List<Object[]> countByStatusRaw();

    @Modifying
    @Query("UPDATE LotPurchase l SET l.status = :status WHERE l.id = :id")
    void updateStatus(Long id, String status);

    default List<MonthlySpend> getMonthlySpend(int months) {
        return getMonthlySpendRaw(months).stream()
                .map(row -> new MonthlySpend(
                        (String) row[0],
                        ((Number) row[1]).doubleValue(),
                        ((Number) row[2]).longValue()))
                .toList();
    }

    default List<LotStatusCount> countByStatus() {
        return countByStatusRaw().stream()
                .map(row -> new LotStatusCount((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }
}
