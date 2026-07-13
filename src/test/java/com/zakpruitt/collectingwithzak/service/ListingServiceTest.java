package com.zakpruitt.collectingwithzak.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListingServiceTest {

    @Test
    void undercutsLowestCompByPercent() {
        // 5% under a $100 comp, market floor far below
        assertEquals(95.00, ListingService.suggestPrice(100.0, 50.0, 5, 80), 0.001);
    }

    @Test
    void floorStopsJunkCompsFromTankingThePrice() {
        // $4 junk comp on a $50 card: floor = 80% of market
        assertEquals(40.00, ListingService.suggestPrice(4.0, 50.0, 5, 80), 0.001);
    }

    @Test
    void noCompsFallsBackToMarketPrice() {
        assertEquals(12.34, ListingService.suggestPrice(null, 12.34, 5, 80), 0.001);
    }

    @Test
    void roundsHalfUpToCents() {
        // 77.35 * 0.95 = 73.4825 -> 73.48
        assertEquals(73.48, ListingService.suggestPrice(77.35, 10.0, 5, 80), 0.001);
    }

    @Test
    void defaultTitleJoinsPartsAndSkipsBlanks() {
        assertEquals("Charizard ex 199/165 151 Pokemon TCG",
                ListingService.defaultTitle("Charizard ex", "199/165", "151"));
        assertEquals("Charizard ex Pokemon TCG",
                ListingService.defaultTitle("Charizard ex", null, ""));
    }

    @Test
    void defaultTitleTruncatesTo80Chars() {
        String longName = "A".repeat(100);
        assertEquals(80, ListingService.defaultTitle(longName, "1/1", "Set").length());
    }
}
