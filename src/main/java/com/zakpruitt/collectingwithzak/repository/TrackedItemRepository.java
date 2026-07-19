package com.zakpruitt.collectingwithzak.repository;

import com.zakpruitt.collectingwithzak.dto.common.InventoryTotals;
import com.zakpruitt.collectingwithzak.dto.common.LabeledStat;
import com.zakpruitt.collectingwithzak.entity.TrackedItem;
import com.zakpruitt.collectingwithzak.entity.enums.ItemStatus;
import com.zakpruitt.collectingwithzak.entity.enums.Purpose;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface TrackedItemRepository extends JpaRepository<TrackedItem, Long> {

    // Items from a lot that is no longer accepted are hidden. One spelling for the rule,
    // so the queries below can't drift apart.
    String VISIBLE_LOT = "(lp IS NULL OR lp.status = 'ACCEPTED')";

    // lotPurchase must be LEFT JOINed explicitly: dereferencing t.lotPurchase.status in the
    // WHERE clause creates an implicit inner join that drops items with no lot.
    @EntityGraph(attributePaths = {"pokemonCard", "sealedProduct", "lotPurchase", "gradingSubmission"})
    @Query("SELECT t FROM TrackedItem t LEFT JOIN t.lotPurchase lp " +
            "WHERE t.purpose = :purpose AND t.status = 'AVAILABLE' AND " + VISIBLE_LOT)
    List<TrackedItem> findByPurpose(Purpose purpose);

    @EntityGraph(attributePaths = {"pokemonCard", "sealedProduct", "lotPurchase", "gradingSubmission"})
    @Query("SELECT t FROM TrackedItem t LEFT JOIN t.lotPurchase lp WHERE t.status = :status " +
            "AND " + VISIBLE_LOT)
    List<TrackedItem> findByStatus(ItemStatus status);

    // Item pickers and the edit form render name/set/image only — no lot or submission links.
    @EntityGraph(attributePaths = {"pokemonCard", "sealedProduct"})
    List<TrackedItem> findByStatusAndSaleIsNull(ItemStatus status);

    // Edit pickers show available inventory plus the already-attached items so they can be re-picked.
    default List<TrackedItem> findAvailablePlus(List<TrackedItem> attachedItems) {
        List<TrackedItem> items = new ArrayList<>(findByStatusAndSaleIsNull(ItemStatus.AVAILABLE));
        items.addAll(attachedItems);
        return items;
    }

    @EntityGraph(attributePaths = {"pokemonCard", "sealedProduct"})
    Optional<TrackedItem> findWithDetailsById(Long id);

    void deleteByLotPurchaseId(Long lotPurchaseId);

    @Query("SELECT COUNT(t) FROM TrackedItem t LEFT JOIN t.lotPurchase lp WHERE t.status = :status " +
            "AND " + VISIBLE_LOT)
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
