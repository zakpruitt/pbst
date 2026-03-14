package com.zakpruitt.pbst.repositories;

import com.zakpruitt.pbst.entities.TrackedItem;
import com.zakpruitt.pbst.enums.ItemGradingStatus;
import com.zakpruitt.pbst.enums.ItemType;
import com.zakpruitt.pbst.enums.Purpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TrackedItemRepository extends JpaRepository<TrackedItem, Long> {

    @Query("SELECT t FROM TrackedItem t WHERE t.lotPurchase.status = 'ACCEPTED'")
    List<TrackedItem> findAllAccepted();

    @Query("SELECT t FROM TrackedItem t WHERE t.purpose = :purpose AND t.lotPurchase.status = 'ACCEPTED'")
    List<TrackedItem> findByPurposeAccepted(Purpose purpose);

    @Query("SELECT COUNT(t) FROM TrackedItem t WHERE t.purpose = :purpose AND t.lotPurchase.status = 'ACCEPTED'")
    long countByPurposeAccepted(Purpose purpose);

    @Query("SELECT SUM(t.pokemonCard.marketPrice) FROM TrackedItem t WHERE t.sale IS NULL AND t.lotPurchase.status = 'ACCEPTED'")
    BigDecimal sumUnsoldMarketValue();

    @Query("SELECT SUM(t.costBasis) FROM TrackedItem t WHERE t.purpose = :purpose AND t.lotPurchase.status = 'ACCEPTED'")
    BigDecimal sumCostBasisByPurpose(Purpose purpose);

    @Query("SELECT SUM(t.pokemonCard.marketPrice) FROM TrackedItem t WHERE t.gradingStatus = :status AND t.lotPurchase.status = 'ACCEPTED'")
    BigDecimal sumMarketValueByGradingStatus(ItemGradingStatus status);

    @Query("SELECT SUM(t.pokemonCard.marketPrice) FROM TrackedItem t WHERE t.purpose = :purpose AND t.sale IS NULL AND t.lotPurchase.status = 'ACCEPTED'")
    BigDecimal sumUnsoldMarketValueByPurpose(Purpose purpose);

    // Keep these for internal logic where we might want everything (e.g. deleting a lot)
    List<TrackedItem> findByPurpose(Purpose purpose);
    List<TrackedItem> findByPurposeAndItemType(Purpose purpose, ItemType itemType);
    List<TrackedItem> findByGradingSubmissionId(Long submissionId);
    long countByPurpose(Purpose purpose);
    List<TrackedItem> findByGradingStatus(ItemGradingStatus status);

    @Query("SELECT t FROM TrackedItem t WHERE (t.gradingStatus = :status OR (t.purpose = 'TO_GRADE' AND t.gradingStatus IS NULL)) AND t.lotPurchase.status = 'ACCEPTED'")
    List<TrackedItem> findItemsToGradeAccepted(ItemGradingStatus status);
    
    List<TrackedItem> findBySaleId(Long saleId);
}
