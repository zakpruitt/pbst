package com.zakpruitt.collectingwithzak.ebay;

import com.zakpruitt.collectingwithzak.entity.LotPurchase;
import com.zakpruitt.collectingwithzak.entity.enums.LotStatus;
import com.zakpruitt.collectingwithzak.repository.LotPurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock
    private JbayProvider jbayProvider;
    @Mock
    private ListingProperties properties;
    @Mock
    private EbayListingRepository listingRepo;
    @Mock
    private LotPurchaseRepository lotRepo;

    private ListingService listingService;

    @BeforeEach
    void setUp() {
        listingService = new ListingService(jbayProvider, properties, listingRepo, lotRepo);
    }

    private static LotPurchase acceptedLot(Long id, String snapshotJson) {
        LotPurchase lot = new LotPurchase();
        lot.setId(id);
        lot.setStatus(LotStatus.ACCEPTED);
        lot.setSellerName("someone");
        lot.setLotContentSnapshot(snapshotJson);
        return lot;
    }

    private static String snapshotJson(SnapshotRow... rows) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < rows.length; i++) {
            if (i > 0) json.append(",");
            json.append("{\"name\":\"").append(rows[i].name)
                    .append("\",\"item_type\":\"").append(rows[i].itemType)
                    .append("\",\"is_tracked\":").append(rows[i].tracked)
                    .append("}");
        }
        return json.append("]").toString();
    }

    private record SnapshotRow(String name, String itemType, boolean tracked) {
    }

    @Test
    void getIndexData_withMixedSnapshot_returnsOnlyUntrackedRawCards() {
        when(lotRepo.findByStatusOrderByPurchaseDateDesc(LotStatus.ACCEPTED)).thenReturn(List.of(
                acceptedLot(1L, snapshotJson(
                        new SnapshotRow("Flip Card", "RAW_CARD", false),
                        new SnapshotRow("Watch Card", "RAW_CARD", true),
                        new SnapshotRow("Graded Thing", "GRADED_CARD", false),
                        new SnapshotRow("Sealed Thing", "SEALED_PRODUCT", false)))));
        when(listingRepo.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        ListingIndexData data = listingService.getIndexData();

        assertThat(data.candidates()).hasSize(1);
        ListingCandidate candidate = data.candidates().getFirst();
        assertThat(candidate.getName()).isEqualTo("Flip Card");
        assertThat(candidate.getKey()).isEqualTo("1:0");
    }

    @Test
    void getIndexData_whenCardAlreadyStaged_excludesItFromCandidates() {
        LotPurchase lot = acceptedLot(1L, snapshotJson(
                new SnapshotRow("Already Listed", "RAW_CARD", false),
                new SnapshotRow("Still Waiting", "RAW_CARD", false)));
        when(lotRepo.findByStatusOrderByPurchaseDateDesc(LotStatus.ACCEPTED)).thenReturn(List.of(lot));

        EbayListing staged = EbayListing.builder().lotPurchase(lot).snapshotIndex(0).build();
        when(listingRepo.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(staged));

        ListingIndexData data = listingService.getIndexData();

        assertThat(data.candidates())
                .extracting(ListingCandidate::getName)
                .containsExactly("Still Waiting");
    }

    @Test
    void findCandidatesByKeys_withUnknownKey_returnsOnlyRealCandidates() {
        when(lotRepo.findByStatusOrderByPurchaseDateDesc(LotStatus.ACCEPTED)).thenReturn(List.of(
                acceptedLot(1L, snapshotJson(new SnapshotRow("Flip Card", "RAW_CARD", false)))));

        List<ListingCandidate> picked = listingService.findCandidatesByKeys(List.of("1:0", "99:5"));

        assertThat(picked)
                .extracting(ListingCandidate::getName)
                .containsExactly("Flip Card");
    }

    @Test
    void suggestPrice_whenCompAboveFloor_undercutsByPercent() {
        // 5% under a $100 comp, market floor far below
        assertThat(ListingService.suggestPrice(100.0, 50.0, 5, 80)).isCloseTo(95.00, within(0.001));
    }

    @Test
    void suggestPrice_whenCompBelowFloor_returnsFloorOfMarket() {
        // $4 junk comp on a $50 card: floor = 80% of market
        assertThat(ListingService.suggestPrice(4.0, 50.0, 5, 80)).isCloseTo(40.00, within(0.001));
    }

    @Test
    void suggestPrice_whenNoComps_fallsBackToMarketPrice() {
        assertThat(ListingService.suggestPrice(null, 12.34, 5, 80)).isCloseTo(12.34, within(0.001));
    }

    @Test
    void suggestPrice_whenFractionalCents_roundsHalfUpToCents() {
        // 77.35 * 0.95 = 73.4825 -> 73.48
        assertThat(ListingService.suggestPrice(77.35, 10.0, 5, 80)).isCloseTo(73.48, within(0.001));
    }

    @Test
    void defaultTitle_withAllParts_joinsThemInOrder() {
        assertThat(ListingService.defaultTitle("Charizard ex", "199/165", "151"))
                .isEqualTo("Charizard ex 199/165 151 Pokemon TCG");
    }

    @Test
    void defaultTitle_withBlankParts_skipsThem() {
        assertThat(ListingService.defaultTitle("Charizard ex", null, ""))
                .isEqualTo("Charizard ex Pokemon TCG");
    }

    @Test
    void defaultTitle_whenLongerThan80Chars_truncates() {
        String longName = "A".repeat(100);
        assertThat(ListingService.defaultTitle(longName, "1/1", "Set")).hasSize(80);
    }
}
