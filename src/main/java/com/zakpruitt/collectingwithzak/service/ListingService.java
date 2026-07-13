package com.zakpruitt.collectingwithzak.service;

import com.zakpruitt.collectingwithzak.config.JbayProvider;
import com.zakpruitt.collectingwithzak.config.ListingProperties;
import com.zakpruitt.collectingwithzak.dto.common.ListingCandidate;
import com.zakpruitt.collectingwithzak.dto.common.ListingReviewRow;
import com.zakpruitt.collectingwithzak.dto.request.StageListingRow;
import com.zakpruitt.collectingwithzak.dto.request.StageListingsRequest;
import com.zakpruitt.collectingwithzak.entity.EbayListing;
import com.zakpruitt.collectingwithzak.entity.enums.ListingStatus;
import com.zakpruitt.collectingwithzak.exception.ResourceNotFoundException;
import com.zakpruitt.collectingwithzak.repository.EbayListingRepository;
import com.zakpruitt.collectingwithzak.repository.LotPurchaseRepository;
import com.zakpruitt.collectingwithzak.service.render.ListingRenderService;
import com.zakpruitt.jbay.Amount;
import com.zakpruitt.jbay.JbayException;
import com.zakpruitt.jbay.account.SellingPolicy;
import com.zakpruitt.jbay.browse.ItemSummary;
import com.zakpruitt.jbay.inventory.InventoryItem;
import com.zakpruitt.jbay.inventory.InventoryLocation;
import com.zakpruitt.jbay.inventory.OfferCreated;
import com.zakpruitt.jbay.inventory.OfferPublished;
import com.zakpruitt.jbay.inventory.OfferRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ListingService {

    // eBay condition for a raw (ungraded) trading card, verified against the live API.
    // Category 183454 also demands the "Card Condition" descriptor (40001) on publish;
    // 400010 = "Near Mint or Better", the default for raw cards pulled from lots.
    private static final String UNGRADED_CONDITION = "USED_VERY_GOOD";
    private static final String CARD_CONDITION_DESCRIPTOR = "40001";
    private static final String CARD_CONDITION_NEAR_MINT_OR_BETTER = "400010";
    private static final int TITLE_MAX_LENGTH = 80;

    private final JbayProvider jbayProvider;
    private final ListingProperties properties;
    private final ListingRenderService listingRenderService;
    private final EbayListingRepository listingRepo;
    private final LotPurchaseRepository lotRepo;

    public record StageResult(int created, List<String> failures) {
    }

    /** Comp research for the selected candidates: lowest delivered price and a suggested undercut. */
    public List<ListingReviewRow> buildReview(List<String> selectedKeys) {
        List<ListingReviewRow> rows = new ArrayList<>();

        for (ListingCandidate candidate : listingRenderService.findCandidatesByKeys(selectedKeys)) {
            List<ItemSummary> comps = searchComps(candidate);
            ItemSummary cheapest = comps.stream()
                    .min(Comparator.comparingDouble(ItemSummary::deliveredPrice))
                    .orElse(null);
            Double compPrice = cheapest == null ? null : cheapest.deliveredPrice();

            rows.add(ListingReviewRow.builder()
                    .candidate(candidate)
                    .compPrice(compPrice)
                    .compTitle(cheapest == null ? "" : cheapest.title())
                    .compUrl(cheapest == null ? "" : cheapest.itemWebUrl())
                    .compCount(comps.size())
                    .suggestedPrice(suggestPrice(compPrice, candidate.getMarketPrice(),
                            properties.undercutPercent(), properties.floorPercent()))
                    .title(defaultTitle(candidate.getName(), candidate.getCardNumber(), candidate.getSetName()))
                    .build());
        }

        return rows;
    }

    /** Stages one inventory item + unpublished offer per row; failures skip the row, not the batch. */
    public StageResult stageListings(StageListingsRequest request) {
        OfferRequest.ListingPolicies policies;
        String merchantLocationKey;
        try {
            policies = resolvePolicies();
            merchantLocationKey = resolveMerchantLocationKey();
        } catch (JbayException | IllegalStateException e) {
            return new StageResult(0, List.of("eBay account setup problem: " + e.getMessage()));
        }

        int created = 0;
        List<String> failures = new ArrayList<>();

        for (StageListingRow row : request.getRows()) {
            if (listingRepo.existsByLotPurchaseIdAndSnapshotIndex(row.getLotPurchaseId(), row.getSnapshotIndex())) {
                failures.add(row.getName() + ": already staged");
                continue;
            }
            try {
                created += stageOne(row, policies, merchantLocationKey);
            } catch (JbayException e) {
                log.warn("Staging '{}' failed: {}", row.getName(), e.getMessage());
                failures.add(row.getName() + ": " + shortMessage(e));
            }
        }

        return new StageResult(created, failures);
    }

    /** Publishes a staged offer — the listing goes live on eBay. */
    public void publish(Long id) {
        EbayListing listing = findById(id);
        if (listing.getStatus() != ListingStatus.STAGED) {
            throw new IllegalArgumentException("Listing " + id + " is not staged");
        }
        OfferPublished published = jbayProvider.client().inventory().publishOffer(listing.getOfferId());
        listing.setEbayListingId(published.listingId());
        listing.setStatus(ListingStatus.PUBLISHED);
        listingRepo.save(listing);
    }

    /** Withdraws a staged listing from eBay and re-exposes the card as a candidate. */
    public void delete(Long id) {
        EbayListing listing = findById(id);
        if (listing.getStatus() == ListingStatus.PUBLISHED) {
            throw new IllegalArgumentException("Listing " + id + " is published — end it on eBay instead");
        }
        try {
            jbayProvider.client().inventory().deleteInventoryItem(listing.getSku());
        } catch (JbayException e) {
            // Already gone on eBay's side is fine — the local row is the source of truth here.
            log.warn("Deleting eBay inventory item {} failed: {}", listing.getSku(), e.getMessage());
        }
        listingRepo.delete(listing);
    }

    static double suggestPrice(Double compPrice, double marketPrice, double undercutPercent, double floorPercent) {
        double suggested = compPrice == null
                ? marketPrice
                : Math.max(compPrice * (1 - undercutPercent / 100), marketPrice * floorPercent / 100);
        return BigDecimal.valueOf(suggested).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    static String defaultTitle(String name, String cardNumber, String setName) {
        String title = String.join(" ", List.of(
                        name == null ? "" : name,
                        cardNumber == null ? "" : cardNumber,
                        setName == null ? "" : setName,
                        "Pokemon TCG"))
                .replaceAll("\\s+", " ")
                .trim();
        return title.length() <= TITLE_MAX_LENGTH ? title : title.substring(0, TITLE_MAX_LENGTH).trim();
    }

    private int stageOne(StageListingRow row, OfferRequest.ListingPolicies policies, String merchantLocationKey) {
        String sku = "PBST-" + row.getLotPurchaseId() + "-" + row.getSnapshotIndex();

        jbayProvider.client().inventory().createOrReplaceInventoryItem(sku, new InventoryItem(
                new InventoryItem.Product(
                        row.getTitle(),
                        row.getTitle() + " — from the Collecting with Zak inventory.",
                        row.getImageUrl() == null || row.getImageUrl().isBlank() ? null : List.of(row.getImageUrl()),
                        aspects(row)),
                UNGRADED_CONDITION,
                List.of(new InventoryItem.ConditionDescriptor(
                        CARD_CONDITION_DESCRIPTOR, List.of(CARD_CONDITION_NEAR_MINT_OR_BETTER))),
                InventoryItem.Availability.quantity(row.getQty())));

        OfferCreated offer = jbayProvider.client().inventory().createOffer(new OfferRequest(
                sku,
                properties.marketplace(),
                "FIXED_PRICE",
                row.getQty(),
                properties.categoryId(),
                row.getTitle(),
                new OfferRequest.PricingSummary(new Amount(String.format(Locale.US, "%.2f", row.getPrice()), "USD")),
                policies,
                merchantLocationKey));

        listingRepo.save(EbayListing.builder()
                .lotPurchase(lotRepo.getReferenceById(row.getLotPurchaseId()))
                .snapshotIndex(row.getSnapshotIndex())
                .cardName(row.getName())
                .setName(row.getSetName() == null ? "" : row.getSetName())
                .cardNumber(row.getCardNumber() == null ? "" : row.getCardNumber())
                .imageUrl(row.getImageUrl() == null ? "" : row.getImageUrl())
                .qty(row.getQty())
                .marketPrice(row.getMarketPrice())
                .compPrice(row.getCompPrice())
                .listedPrice(row.getPrice())
                .sku(sku)
                .offerId(offer.offerId())
                .build());

        return 1;
    }

    private List<ItemSummary> searchComps(ListingCandidate candidate) {
        String query = String.join(" ", List.of(
                        "pokemon",
                        candidate.getName() == null ? "" : candidate.getName(),
                        candidate.getSetName() == null ? "" : candidate.getSetName(),
                        candidate.getCardNumber() == null ? "" : candidate.getCardNumber()))
                .replaceAll("\\s+", " ")
                .trim();
        String excludeSeller = properties.excludeSeller();
        try {
            return jbayProvider.client().browse()
                    .searchFixedPrice(query, properties.categoryId(), properties.compLimit())
                    .stream()
                    .filter(item -> excludeSeller == null || excludeSeller.isBlank()
                            || !item.sellerUsername().equalsIgnoreCase(excludeSeller))
                    .toList();
        } catch (JbayException e) {
            log.warn("Comp search for '{}' failed: {}", query, e.getMessage());
            return List.of();
        }
    }

    private Map<String, List<String>> aspects(StageListingRow row) {
        Map<String, List<String>> aspects = new java.util.LinkedHashMap<>();
        aspects.put("Game", List.of("Pokémon TCG"));
        aspects.put("Language", List.of("English"));
        aspects.put("Graded", List.of("No"));
        if (row.getName() != null && !row.getName().isBlank()) {
            aspects.put("Card Name", List.of(row.getName()));
        }
        if (row.getSetName() != null && !row.getSetName().isBlank()) {
            aspects.put("Set", List.of(row.getSetName()));
        }
        if (row.getCardNumber() != null && !row.getCardNumber().isBlank()) {
            aspects.put("Card Number", List.of(row.getCardNumber()));
        }
        return aspects;
    }

    private OfferRequest.ListingPolicies resolvePolicies() {
        String marketplace = properties.marketplace();
        SellingPolicy payment = firstPolicy(jbayProvider.client().account().paymentPolicies(marketplace), "payment");
        SellingPolicy returns = firstPolicy(jbayProvider.client().account().returnPolicies(marketplace), "return");
        SellingPolicy fulfillment = firstPolicy(jbayProvider.client().account().fulfillmentPolicies(marketplace), "fulfillment");
        return new OfferRequest.ListingPolicies(payment.policyId(), returns.policyId(), fulfillment.policyId());
    }

    private static SellingPolicy firstPolicy(List<SellingPolicy> policies, String kind) {
        if (policies.isEmpty()) {
            throw new IllegalStateException("no " + kind
                    + " policy found — set up business policies in eBay Seller Hub");
        }
        return policies.getFirst();
    }

    private String resolveMerchantLocationKey() {
        List<InventoryLocation> locations = jbayProvider.client().inventory().locations();
        if (locations.isEmpty()) {
            throw new IllegalStateException("no eBay merchant location found — create one "
                    + "(Seller Hub > Shipping preferences, or the Inventory API createInventoryLocation call)");
        }
        return locations.getFirst().merchantLocationKey();
    }

    private static String shortMessage(JbayException e) {
        String body = e.responseBody();
        return body == null || body.isBlank() ? e.getMessage()
                : body.length() > 300 ? body.substring(0, 300) : body;
    }

    private EbayListing findById(Long id) {
        return listingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EbayListing", id));
    }
}
