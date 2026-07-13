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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingRenderServiceTest {

    @Mock
    private LotPurchaseRepository lotRepo;
    @Mock
    private EbayListingRepository listingRepo;
    @Mock
    private EbayListingMapper listingMapper;
    @Mock
    private JbayProvider jbayProvider;

    private ListingRenderService renderService;

    @BeforeEach
    void setUp() {
        renderService = new ListingRenderService(lotRepo, listingRepo, listingMapper, jbayProvider);
        lenient().when(listingMapper.toResponseList(any())).thenReturn(List.of());
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

        ListingIndexData data = renderService.getIndexData();

        assertThat(data.getCandidates()).hasSize(1);
        ListingCandidate candidate = data.getCandidates().getFirst();
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

        ListingIndexData data = renderService.getIndexData();

        assertThat(data.getCandidates())
                .extracting(ListingCandidate::getName)
                .containsExactly("Still Waiting");
    }

    @Test
    void findCandidatesByKeys_withUnknownKey_returnsOnlyRealCandidates() {
        when(lotRepo.findByStatusOrderByPurchaseDateDesc(LotStatus.ACCEPTED)).thenReturn(List.of(
                acceptedLot(1L, snapshotJson(new SnapshotRow("Flip Card", "RAW_CARD", false)))));

        List<ListingCandidate> picked = renderService.findCandidatesByKeys(List.of("1:0", "99:5"));

        assertThat(picked)
                .extracting(ListingCandidate::getName)
                .containsExactly("Flip Card");
    }
}
