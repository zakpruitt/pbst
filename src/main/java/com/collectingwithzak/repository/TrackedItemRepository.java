package com.collectingwithzak.repository;

import com.collectingwithzak.dto.response.ItemTypeCount;
import com.collectingwithzak.entity.TrackedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TrackedItemRepository extends JpaRepository<TrackedItem, Long> {

    // ---------- Business logic ----------

    @Query("SELECT t FROM TrackedItem t " +
           "LEFT JOIN FETCH t.pokemonCard LEFT JOIN FETCH t.sealedProduct " +
           "LEFT JOIN FETCH t.lotPurchase LEFT JOIN FETCH t.gradingSubmission " +
           "WHERE t.purpose = :purpose " +
           "AND (t.lotPurchase IS NULL OR t.lotPurchase.status = 'ACCEPTED')")
    List<TrackedItem> findByPurpose(String purpose);

    @Query("SELECT t FROM TrackedItem t LEFT JOIN FETCH t.pokemonCard " +
           "WHERE t.purpose = 'INVENTORY' AND t.sale IS NULL")
    List<TrackedItem> findAvailableInventory();

    @Modifying
    @Query("UPDATE TrackedItem t SET t.gradingSubmission.id = :submissionId WHERE t.id IN :itemIds")
    void attachToSubmission(List<Long> itemIds, Long submissionId);

    @Modifying
    @Query("UPDATE TrackedItem t SET t.gradingSubmission = NULL, t.purpose = 'INVENTORY' " +
           "WHERE t.gradingSubmission.id = :submissionId")
    void detachFromSubmission(Long submissionId);

    @Modifying
    @Query("UPDATE TrackedItem t SET t.purpose = :purpose WHERE t.gradingSubmission.id = :submissionId")
    void updatePurposeBySubmission(Long submissionId, String purpose);

    @Modifying
    @Query("UPDATE TrackedItem t SET t.sale.id = :saleId, t.purpose = 'SOLD' WHERE t.id IN :itemIds")
    void attachToSale(List<Long> itemIds, Long saleId);

    @Modifying
    @Query("UPDATE TrackedItem t SET t.sale = NULL, t.purpose = 'INVENTORY' WHERE t.sale.id = :saleId")
    void detachFromSale(Long saleId);

    void deleteByLotPurchaseId(Long lotPurchaseId);

    // ---------- Dashboard / KPI ----------

    @Query("SELECT COUNT(t) FROM TrackedItem t WHERE t.purpose = :purpose " +
           "AND (t.lotPurchase IS NULL OR t.lotPurchase.status = 'ACCEPTED')")
    long countByPurpose(String purpose);

    @Query(value = "SELECT item_type, COUNT(*), " +
                   "COALESCE(SUM(market_value_at_purchase), 0), COALESCE(SUM(cost_basis), 0) " +
                   "FROM tracked_items WHERE purpose = 'INVENTORY' AND sale_id IS NULL AND deleted_at IS NULL " +
                   "GROUP BY item_type", nativeQuery = true)
    List<Object[]> countByItemTypeRaw();

    @Query(value = "SELECT COALESCE(SUM(cost_basis), 0), COALESCE(SUM(market_value_at_purchase), 0) " +
                   "FROM tracked_items WHERE purpose = 'INVENTORY' AND sale_id IS NULL AND deleted_at IS NULL", nativeQuery = true)
    List<Object[]> getInventoryTotalsRaw();

    default List<ItemTypeCount> countByItemType() {
        return countByItemTypeRaw().stream()
                .map(row -> new ItemTypeCount(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).doubleValue(),
                        ((Number) row[3]).doubleValue()))
                .toList();
    }

    default double[] getInventoryTotals() {
        Object[] row = getInventoryTotalsRaw().getFirst();
        return new double[]{((Number) row[0]).doubleValue(), ((Number) row[1]).doubleValue()};
    }
}
