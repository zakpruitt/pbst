package com.zakpruitt.pbst.controllers;

import com.zakpruitt.pbst.entities.PokemonCard;
import com.zakpruitt.pbst.repositories.PokemonCardRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/test")
@AllArgsConstructor
public class TestController {

    private final PokemonCardRepository pokemonCardRepository;

    @PostMapping("/fix-images")
    public ResponseEntity<String> fixImages() {
        List<PokemonCard> cards = pokemonCardRepository.findAll();
        int count = 0;
        for (PokemonCard card : cards) {
            if (card.getImageUrl() == null) {
                // We don't have the TCGPlayer URL stored in the entity, only in the DTO during sync.
                // This is a problem. We can't reconstruct it if we didn't save the TCGPlayer ID or URL.
                
                // Wait, we don't store the TCGPlayer URL in the PokemonCard entity.
                // We only mapped it during sync.
                
                // If the data is already in the DB without the URL, we are stuck unless we re-sync.
            }
        }
        return ResponseEntity.ok("Cannot fix images without re-syncing. Please run the sync job.");
    }
}
