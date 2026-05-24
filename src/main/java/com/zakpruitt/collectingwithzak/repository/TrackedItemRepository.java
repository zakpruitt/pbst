package com.zakpruitt.collectingwithzak.repository;

import com.zakpruitt.collectingwithzak.dto.common.InventoryTotals;
import com.zakpruitt.collectingwithzak.dto.common.LabeledStat;
import com.zakpruitt.collectingwithzak.entity.TrackedItem;
import com.zakpruitt.collectingwithzak.entity.enums.ItemStatus;
import com.zakpruitt.collectingwithzak.entity.enums.Purpose;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TrackedItemRepository extends JpaRepository<TrackedItem, Long> {

    @EntityGraph(attributePaths = {"pokemonCard", "sealedProduct", "lotPurchase", "gradingSubmission"})
    @Query("SELECT t FROM TrackedItem t WHERE t.purpose = :purpose AND t.status = 'AVAILABLE' " +
            "AND (t.lotPurchase IS NULL OR t.lotPurchase.status = 'ACCEPTED')")
    List<TrackedItem> findByPurpose(Purpose purpose);

    @EntityGraph(attributePaths = {"pokemonCard", "sealedProduct", "lotPurchase", "gradingSubmission"})
    @Query("SELECT t FROM TrackedItem t WHERE t.status = :status " +
            "AND (t.lotPurchase IS NULL OR t.lotPurchase.status = 'ACCEPTED')")
    List<TrackedItem> findByStatus(ItemStatus status);

    @EntityGraph(attributePaths = {"pokemonCard"})
    List<TrackedItem> findByStatusAndSaleIsNull(ItemStatus status);

    void deleteByLotPurchaseId(Long lotPurchaseId);

    @Query("SELECT COUNT(t) FROM TrackedItem t WHERE t.status = :status " +
            "AND (t.lotPurchase IS NULL OR t.lotPurchase.status = 'ACCEPTED')")
    long countByStatus(ItemStatus status);

    @Query("SELECT new com.zakpruitt.collectingwithzak.dto.common.LabeledStat(t.itemType, COUNT(t)) " +
            "FROM TrackedItem t WHERE t.status = 'AVAILABLE' AND t.sale IS NULL " +
            "GROUP BY t.itemType")
    List<LabeledStat> countByItemType();

    @Query("SELECT new com.zakpruitt.collectingwithzak.dto.common.InventoryTotals(" +
            "COALESCE(SUM(t.costBasis), 0.0), COALESCE(SUM(t.marketValueAtPurchase), 0.0)) " +
            "FROM TrackedItem t WHERE t.status = 'AVAILABLE' AND t.sale IS NULL")
    InventoryTotals getInventoryTotals();
}
