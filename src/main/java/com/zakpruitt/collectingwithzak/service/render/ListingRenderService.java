package com.zakpruitt.collectingwithzak.service.render;

import com.zakpruitt.collectingwithzak.config.JbayProvider;
import com.zakpruitt.collectingwithzak.dto.common.ListingCandidate;
import com.zakpruitt.collectingwithzak.dto.render.ListingIndexData;
import com.zakpruitt.collectingwithzak.dto.request.SnapshotItem;
import com.zakpruitt.collectingwithzak.entity.EbayListing;
import com.zakpruitt.collectingwithzak.entity.LotPurchase;
import com.zakpruitt.collectingwithzak.entity.enums.LotStatus;
import com.zakpruitt.collectingwithzak.mapper.EbayListingMapper;
import com.zakpruitt.collectingwithzak.repository.EbayListingRepository;
import com.zakpruitt.collectingwithzak.repository.LotPurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListingRenderService {

    private final LotPurchaseRepository lotRepo;
    private final EbayListingRepository listingRepo;
    private final EbayListingMapper listingMapper;
    private final JbayProvider jbayProvider;

    public ListingIndexData getIndexData() {
        List<EbayListing> listings = listingRepo.findAllByOrderByCreatedAtDesc();
        Set<String> listedKeys = listings.stream()
                .map(listing -> candidateKey(listing.getLotPurchase().getId(), listing.getSnapshotIndex()))
                .collect(Collectors.toSet());

        return ListingIndexData.builder()
                .candidates(findCandidates(listedKeys))
                .listings(listingMapper.toResponseList(listings))
                .ebayConfigured(jbayProvider.isConfigured())
                .build();
    }

    /** The selected candidates, re-derived server-side so stale or forged keys are dropped. */
    public List<ListingCandidate> findCandidatesByKeys(List<String> keys) {
        Set<String> wanted = Set.copyOf(keys);
        return findCandidates(Set.of()).stream()
                .filter(candidate -> wanted.contains(candidate.getKey()))
                .toList();
    }

    /** Untracked raw cards from accepted lots — the quick-flip pile — minus already-listed keys. */
    private List<ListingCandidate> findCandidates(Set<String> excludedKeys) {
        List<ListingCandidate> candidates = new ArrayList<>();

        for (LotPurchase lot : lotRepo.findByStatusOrderByPurchaseDateDesc(LotStatus.ACCEPTED)) {
            List<SnapshotItem> snapshot = lot.parseSnapshot();
            for (int index = 0; index < snapshot.size(); index++) {
                SnapshotItem item = snapshot.get(index);
                if (item.isTracked() || !"RAW_CARD".equals(item.getItemType())) {
                    continue;
                }
                String key = candidateKey(lot.getId(), index);
                if (excludedKeys.contains(key)) {
                    continue;
                }
                candidates.add(ListingCandidate.builder()
                        .lotPurchaseId(lot.getId())
                        .snapshotIndex(index)
                        .key(key)
                        .lotSellerName(lot.getSellerName())
                        .name(item.getName())
                        .setName(item.getSetName())
                        .cardNumber(item.getCardNumber())
                        .rarity(item.getRarity())
                        .qty(item.getQty())
                        .marketPrice(item.getMarketPrice())
                        .imageUrl(item.getImageUrl())
                        .build());
            }
        }

        return candidates;
    }

    private static String candidateKey(Long lotPurchaseId, int snapshotIndex) {
        return lotPurchaseId + ":" + snapshotIndex;
    }
}
