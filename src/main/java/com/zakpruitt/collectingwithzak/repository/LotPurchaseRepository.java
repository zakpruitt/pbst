package com.zakpruitt.collectingwithzak.repository;

import com.zakpruitt.collectingwithzak.entity.LotPurchase;
import com.zakpruitt.collectingwithzak.entity.enums.LotStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LotPurchaseRepository extends JpaRepository<LotPurchase, Long> {

    // open-in-view is off: fetch everything the lot detail template renders per item.
    @EntityGraph(attributePaths = {"trackedItems", "trackedItems.pokemonCard", "trackedItems.sealedProduct"})
    Optional<LotPurchase> findWithItemsById(Long id);

    @EntityGraph(attributePaths = {"trackedItems"})
    List<LotPurchase> findAllByOrderByPurchaseDateDesc();

    List<LotPurchase> findByStatusOrderByPurchaseDateDesc(LotStatus status);

    List<LotPurchase> findByOrderByPurchaseDateDesc(Pageable pageable);

    @Query("SELECT COALESCE(SUM(l.totalCost), 0) FROM LotPurchase l WHERE l.status != 'REJECTED'")
    double getTotalCostNonRejected();

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('month', purchase_date), 'YYYY-MM') AS month, " +
            "COALESCE(SUM(total_cost), 0) AS spend " +
            "FROM lot_purchases WHERE status != 'REJECTED' " +
            "AND purchase_date >= NOW() - make_interval(months => :months) " +
            "GROUP BY month ORDER BY month", nativeQuery = true)
    List<MonthlySpendRow> getMonthlySpend(int months);

    interface MonthlySpendRow {

        String getMonth();

        double getSpend();
    }
}
