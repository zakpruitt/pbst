package com.zakpruitt.pbst.services;

import com.zakpruitt.pbst.clients.PokeWalletClient;
import com.zakpruitt.pbst.dtos.pokewallet.PokeWalletCardDto;
import com.zakpruitt.pbst.entities.PokemonCard;
import com.zakpruitt.pbst.mappers.PokemonCardMapper;
import com.zakpruitt.pbst.repositories.PokemonCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PokeWalletSyncService {

    private static final List<String> SET_GROUP_1 = List.of(
            "24423", // M-P Promotional Cards
            "24399", // Mega Brave
            "24400", // Mega Symphonia
            "24459",  // Inferno X
            "24499", // High Class Pack: MEGA Dream ex
            "24600"   // Nihil Zero
    );
    private static final List<String> SET_GROUP_2 = List.of(
    );
    private final PokeWalletClient pokeWalletClient;
    private final PokemonCardRepository pokemonCardRepository;
    private final PokemonCardMapper pokemonCardMapper;

    @Scheduled(cron = "${pokewallet.sync.group1.cron}")
    public void syncGroup1Sets() {
        log.info("Starting scheduled sync for Set Group 1...");
        executeSyncForSets(SET_GROUP_1);
    }

    @Scheduled(cron = "${pokewallet.sync.group2.cron}")
    public void syncGroup2Sets() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Transactional
    public void executeSyncForSets(List<String> setCodes) {
        for (String setCode : setCodes) {
            log.info("Syncing set: {}", setCode);

            try {
                List<PokeWalletCardDto> apiCards = pokeWalletClient.fetchAllCardsByQuery(setCode);
                if (apiCards.isEmpty()) {
                    log.warn("No cards returned from API for set {}", setCode);
                    continue;
                }

                List<PokemonCard> entitiesToSave = apiCards.stream()
                        .map(pokemonCardMapper::toEntity)
                        .toList();
                pokemonCardRepository.saveAll(entitiesToSave);
                log.info("Successfully synced and saved {} cards for set {}", entitiesToSave.size(), setCode);

            } catch (Exception e) {
                log.error("Failed to sync set: {}", setCode, e);
            }
        }

        log.info("Finished sync execution for current group.");
    }
}