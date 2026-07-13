package com.zakpruitt.collectingwithzak.entity;

import com.zakpruitt.collectingwithzak.entity.enums.ListingStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * A staged or published eBay listing for one untracked snapshot card. Identity is
 * (lotPurchase, snapshotIndex) — the card's position in the lot's content snapshot.
 * Editing an accepted lot's items can shift indexes; the copied card fields make
 * any drift visible in the listings table.
 */
@Entity
@Table(name = "ebay_listings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EbayListing extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_purchase_id")
    private LotPurchase lotPurchase;

    @Column(name = "snapshot_index")
    private int snapshotIndex;

    @Builder.Default
    @Column(name = "card_name")
    private String cardName = "";

    @Builder.Default
    @Column(name = "set_name")
    private String setName = "";

    @Builder.Default
    @Column(name = "card_number")
    private String cardNumber = "";

    @Builder.Default
    @Column(name = "image_url")
    private String imageUrl = "";

    @Builder.Default
    private int qty = 1;

    @Column(name = "market_price", columnDefinition = "numeric(10,2)")
    private double marketPrice;

    @Column(name = "comp_price", columnDefinition = "numeric(10,2)")
    private Double compPrice;

    @Column(name = "listed_price", columnDefinition = "numeric(10,2)")
    private double listedPrice;

    @Builder.Default
    private String sku = "";

    @Builder.Default
    @Column(name = "offer_id")
    private String offerId = "";

    @Builder.Default
    @Column(name = "ebay_listing_id")
    private String ebayListingId = "";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private ListingStatus status = ListingStatus.STAGED;
}
