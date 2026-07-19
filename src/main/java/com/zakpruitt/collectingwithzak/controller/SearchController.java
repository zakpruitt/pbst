package com.zakpruitt.collectingwithzak.controller;

import com.zakpruitt.collectingwithzak.entity.PokemonCard;
import com.zakpruitt.collectingwithzak.entity.SealedProduct;
import com.zakpruitt.collectingwithzak.service.SearchService;
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

    private final SearchService searchService;

    @GetMapping("/cards/search")
    public List<PokemonCard> searchCards(@RequestParam(defaultValue = "") String q) {
        return searchService.searchCards(q);
    }

    @GetMapping("/sealed/search")
    public List<SealedProduct> searchSealed(@RequestParam(defaultValue = "") String q) {
        return searchService.searchSealed(q);
    }
}
