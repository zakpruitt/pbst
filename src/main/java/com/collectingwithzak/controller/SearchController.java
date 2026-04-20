package com.collectingwithzak.controller;

import com.collectingwithzak.entity.PokemonCard;
import com.collectingwithzak.entity.SealedProduct;
import com.collectingwithzak.repository.PokemonCardRepository;
import com.collectingwithzak.repository.SealedProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SearchController {

    private final PokemonCardRepository cardRepo;
    private final SealedProductRepository sealedRepo;

    @GetMapping("/cards/search")
    public List<PokemonCard> searchCards(@RequestParam(defaultValue = "") String q) {
        if (q.isBlank()) return List.of();
        return cardRepo.search(q);
    }

    @GetMapping("/sealed/search")
    public List<SealedProduct> searchSealed(@RequestParam(defaultValue = "") String q) {
        if (q.isBlank()) return List.of();
        return sealedRepo.search(q);
    }
}
