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

    // lotPurchase must be LEFT JOINed explicitly: dereferencing t.lotPurchase.status in the
    // WHERE clause creates an implicit inner join that drops items with no lot.
    @EntityGraph(attributePaths = {"pokemonCard", "sealedProduct", "lotPurchase", "gradingSubmission"})
    @Query("SELECT t FROM TrackedItem t LEFT JOIN t.lotPurchase lp " +
            "WHERE t.purpose = :purpose AND t.status = 'AVAILABLE' " +
            "AND (lp IS NULL OR lp.status = 'ACCEPTED')")
    List<TrackedItem> findByPurpose(Purpose purpose);

    @EntityGraph(attributePaths = {"pokemonCard", "sealedProduct", "lotPurchase", "gradingSubmission"})
    @Query("SELECT t FROM TrackedItem t LEFT JOIN t.lotPurchase lp WHERE t.status = :status " +
            "AND (lp IS NULL OR lp.status = 'ACCEPTED')")
    List<TrackedItem> findByStatus(ItemStatus status);

    @EntityGraph(attributePaths = {"pokemonCard"})
    List<TrackedItem> findByStatusAndSaleIsNull(ItemStatus status);

    void deleteByLotPurchaseId(Long lotPurchaseId);

    @Query("SELECT COUNT(t) FROM TrackedItem t LEFT JOIN t.lotPurchase lp WHERE t.status = :status " +
            "AND (lp IS NULL OR lp.status = 'ACCEPTED')")
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
