package com.zakpruitt.collectingwithzak.repository;

import com.zakpruitt.collectingwithzak.entity.EbayListing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EbayListingRepository extends JpaRepository<EbayListing, Long> {

    List<EbayListing> findAllByOrderByCreatedAtDesc();

    boolean existsByLotPurchaseIdAndSnapshotIndex(Long lotPurchaseId, int snapshotIndex);
}
