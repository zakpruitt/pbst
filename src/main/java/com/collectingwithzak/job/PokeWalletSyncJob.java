package com.collectingwithzak.job;

import com.collectingwithzak.client.PokeWalletClient;
import com.collectingwithzak.mapper.PokemonCardMapper;
import com.collectingwithzak.repository.PokemonCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PokeWalletSyncJob {

    private static final List<String> SYNC_SETS = List.of(
            "24423",        // M-P Promotional Cards
            "24399",        // Mega Brave
            "24400",        // Mega Symphonia
            "Inferno X",
            "Mega Dream",
            "Nihil Zero",
            "Ninja Spinner"
    );

    private final PokeWalletClient pokeWalletClient;
    private final PokemonCardRepository cardRepo;
    private final PokemonCardMapper cardMapper;

    @Scheduled(fixedRate = 43_200_000)
    public void sync() {
        if (!pokeWalletClient.isConfigured()) {
            log.info("PokeWallet sync skipped: credentials not configured");
            return;
        }

        log.info("PokeWallet sync started: {} sets", SYNC_SETS.size());
        for (String setCode : SYNC_SETS) {
            try {
                syncSet(setCode);
            } catch (Exception e) {
                log.error("Sync failed for set {}", setCode, e);
            }
        }
    }

    @Transactional
    public void syncSet(String setCode) {
        var cards = pokeWalletClient.fetchAllCards(setCode);
        if (cards.isEmpty()) {
            log.info("No cards returned for set: {}", setCode);
            return;
        }

        var entities = cards.stream().map(cardMapper::fromPokeWallet).toList();
        cardRepo.saveAll(entities);
        log.info("Set synced: {} — {} cards", setCode, entities.size());
    }
}
