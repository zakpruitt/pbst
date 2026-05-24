package com.zakpruitt.collectingwithzak.job;

import com.zakpruitt.collectingwithzak.client.PokeWalletClient;
import com.zakpruitt.collectingwithzak.dto.pokewallet.PokeWalletSetsResponse.PokeWalletSet;
import com.zakpruitt.collectingwithzak.mapper.PokemonCardMapper;
import com.zakpruitt.collectingwithzak.repository.PokemonCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class PokeWalletSyncJob {

    private static final int SEGMENT_COUNT = 7;
    private static final Set<String> ALLOWED_LANGUAGES = Set.of("eng", "jap");

    private final PokeWalletClient pokeWalletClient;
    private final PokemonCardRepository cardRepo;
    private final PokemonCardMapper cardMapper;
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void sync() {
        List<PokeWalletSet> allSets = fetchFilteredSets();
        List<PokeWalletSet> segment = getSegment(allSets, LocalDate.now().getDayOfYear());
        log.info("PokeWallet daily sync started: {}/{} sets (day {})",
                segment.size(), allSets.size(), LocalDate.now().getDayOfYear());

        syncSets(segment);
    }

    @Transactional
    public void syncAll() {
        List<PokeWalletSet> allSets = fetchFilteredSets();
        log.info("PokeWallet full sync started: {} sets", allSets.size());
        syncSets(allSets);
    }

    private List<PokeWalletSet> fetchFilteredSets() {
        return pokeWalletClient.fetchSets().stream()
                .filter(s -> ALLOWED_LANGUAGES.contains(s.getLanguage()))
                .sorted(Comparator.comparing(PokeWalletSet::getSetId))
                .toList();
    }

    private List<PokeWalletSet> getSegment(List<PokeWalletSet> allSets, int dayOfYear) {
        int segmentIndex = dayOfYear % SEGMENT_COUNT;
        int chunkSize = (allSets.size() + SEGMENT_COUNT - 1) / SEGMENT_COUNT;
        int start = segmentIndex * chunkSize;
        int end = Math.min(start + chunkSize, allSets.size());
        return allSets.subList(start, end);
    }

    private void syncSets(List<PokeWalletSet> sets) {
        int synced = 0;
        for (PokeWalletSet set : sets) {
            try {
                syncSet(set.getSetId());
                synced++;
            } catch (Exception e) {
                log.error("Sync failed for set {} ({})", set.getName(), set.getSetId(), e);
            }
        }
        log.info("PokeWallet sync complete: {}/{} sets synced", synced, sets.size());
    }

    private void syncSet(String setId) {
        var cards = pokeWalletClient.fetchAllCards(setId);
        if (cards.isEmpty()) {
            log.info("No cards returned for set: {}", setId);
            return;
        }

        var entities = cards.stream()
                .filter(c -> c.getCardInfo() != null && c.getCardInfo().getCardNumber() != null)
                .map(cardMapper::fromPokeWallet)
                .toList();
        cardRepo.saveAll(entities);
        log.info("Set synced: {} — {} cards", setId, entities.size());
    }

}
