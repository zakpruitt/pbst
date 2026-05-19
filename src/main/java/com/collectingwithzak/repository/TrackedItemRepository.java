package com.collectingwithzak.repository;

import com.collectingwithzak.dto.dashboard.LabeledStat;
import com.collectingwithzak.dto.inventory.InventoryTotals;
import com.collectingwithzak.entity.TrackedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TrackedItemRepository extends JpaRepository<TrackedItem, Long> {

    @Query("SELECT t FROM TrackedItem t " +
            "LEFT JOIN FETCH t.pokemonCard LEFT JOIN FETCH t.sealedProduct " +
            "LEFT JOIN FETCH t.lotPurchase LEFT JOIN FETCH t.gradingSubmission " +
            "WHERE t.purpose = :purpose AND t.status = 'AVAILABLE' " +
            "AND (t.lotPurchase IS NULL OR t.lotPurchase.status = 'ACCEPTED')")
    List<TrackedItem> findByPurpose(String purpose);

    @Query("SELECT t FROM TrackedItem t " +
            "LEFT JOIN FETCH t.pokemonCard LEFT JOIN FETCH t.sealedProduct " +
            "LEFT JOIN FETCH t.lotPurchase LEFT JOIN FETCH t.gradingSubmission " +
            "WHERE t.status = :status " +
            "AND (t.lotPurchase IS NULL OR t.lotPurchase.status = 'ACCEPTED')")
    List<TrackedItem> findByStatus(String status);

    @Query("SELECT t FROM TrackedItem t LEFT JOIN FETCH t.pokemonCard " +
            "WHERE t.status = 'AVAILABLE' AND t.sale IS NULL")
    List<TrackedItem> findAvailableInventory();

    @Modifying
    @Query("UPDATE TrackedItem t SET t.gradingSubmission.id = :submissionId WHERE t.id IN :itemIds")
    void attachToSubmission(List<Long> itemIds, Long submissionId);

    @Modifying
    @Query("UPDATE TrackedItem t SET t.gradingSubmission = NULL, t.status = 'AVAILABLE' " +
            "WHERE t.gradingSubmission.id = :submissionId")
    void detachFromSubmission(Long submissionId);

    @Modifying
    @Query("UPDATE TrackedItem t SET t.status = :status WHERE t.gradingSubmission.id = :submissionId")
    void updateStatusBySubmission(Long submissionId, String status);

    @Modifying
    @Query("UPDATE TrackedItem t SET t.sale.id = :saleId, t.status = 'SOLD' WHERE t.id IN :itemIds")
    void attachToSale(List<Long> itemIds, Long saleId);

    @Modifying
    @Query("UPDATE TrackedItem t SET t.sale = NULL, t.status = 'AVAILABLE' WHERE t.sale.id = :saleId")
    void detachFromSale(Long saleId);

    void deleteByLotPurchaseId(Long lotPurchaseId);

    @Query("SELECT COUNT(t) FROM TrackedItem t WHERE t.status = :status " +
            "AND (t.lotPurchase IS NULL OR t.lotPurchase.status = 'ACCEPTED')")
    long countByStatus(String status);

    @Query(value = "SELECT item_type, COUNT(*) " +
            "FROM tracked_items WHERE status = 'AVAILABLE' AND sale_id IS NULL " +
            "GROUP BY item_type", nativeQuery = true)
    List<Object[]> countByItemTypeRaw();

    @Query(value = "SELECT COALESCE(SUM(cost_basis), 0), COALESCE(SUM(market_value_at_purchase), 0) " +
            "FROM tracked_items WHERE status = 'AVAILABLE' AND sale_id IS NULL", nativeQuery = true)
    List<Object[]> getInventoryTotalsRaw();

    default List<LabeledStat> countByItemType() {
        return countByItemTypeRaw().stream()
                .map(row -> new LabeledStat(
                        (String) row[0],
                        ((Number) row[1]).longValue()))
                .toList();
    }

    default InventoryTotals getInventoryTotals() {
        List<Object[]> results = getInventoryTotalsRaw();
        if (results.isEmpty()) return new InventoryTotals(0, 0);
        Object[] row = results.getFirst();
        return new InventoryTotals(
                ((Number) row[0]).doubleValue(),
                ((Number) row[1]).doubleValue());
    }
}
