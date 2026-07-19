package com.zakpruitt.collectingwithzak.repository;

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

    // Items from a lot that is no longer accepted are hidden; one spelling for the rule,
    // so the queries below can't drift apart. lotPurchase must be LEFT JOINed explicitly —
    // dereferencing t.lotPurchase.status in a WHERE clause creates an implicit inner join
    // that drops items with no lot.
    String VISIBLE_LOT = "(lp IS NULL OR lp.status = 'ACCEPTED')";

    @EntityGraph(attributePaths = {"pokemonCard", "sealedProduct"})
    Optional<TrackedItem> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"pokemonCard", "sealedProduct", "lotPurchase", "gradingSubmission"})
    @Query("SELECT t FROM TrackedItem t LEFT JOIN t.lotPurchase lp " +
            "WHERE t.purpose = :purpose AND t.status = 'AVAILABLE' AND " + VISIBLE_LOT)
    List<TrackedItem> findByPurpose(Purpose purpose);

    @EntityGraph(attributePaths = {"pokemonCard", "sealedProduct", "lotPurchase", "gradingSubmission"})
    @Query("SELECT t FROM TrackedItem t LEFT JOIN t.lotPurchase lp " +
            "WHERE t.status = :status AND " + VISIBLE_LOT)
    List<TrackedItem> findByStatus(ItemStatus status);

    // Edit pickers show available inventory plus the already-attached items so they can be re-picked.
    default List<TrackedItem> findAvailablePlus(List<TrackedItem> attachedItems) {
        List<TrackedItem> items = new ArrayList<>(findByStatus(ItemStatus.AVAILABLE));
        items.addAll(attachedItems);
        return items;
    }

    void deleteByLotPurchaseId(Long lotPurchaseId);

    @Query("SELECT COUNT(t) FROM TrackedItem t LEFT JOIN t.lotPurchase lp " +
            "WHERE t.status = :status AND " + VISIBLE_LOT)
    long countByStatus(ItemStatus status);

    @Query("SELECT new com.zakpruitt.collectingwithzak.repository.LabeledStat(t.itemType, COUNT(t)) " +
            "FROM TrackedItem t LEFT JOIN t.lotPurchase lp " +
            "WHERE t.status = 'AVAILABLE' AND " + VISIBLE_LOT + " GROUP BY t.itemType")
    List<LabeledStat> countByItemType();

    @Query("SELECT new com.zakpruitt.collectingwithzak.repository.TrackedItemRepository$InventoryTotals(" +
            "COUNT(t), COALESCE(SUM(t.costBasis), 0.0), COALESCE(SUM(t.marketValueAtPurchase), 0.0)) " +
            "FROM TrackedItem t LEFT JOIN t.lotPurchase lp " +
            "WHERE t.status = 'AVAILABLE' AND " + VISIBLE_LOT)
    InventoryTotals getInventoryTotals();

    record InventoryTotals(long count, double cost, double market) {
    }
}
