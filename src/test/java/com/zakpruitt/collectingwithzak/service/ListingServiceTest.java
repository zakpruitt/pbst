package com.zakpruitt.collectingwithzak.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ListingServiceTest {

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
