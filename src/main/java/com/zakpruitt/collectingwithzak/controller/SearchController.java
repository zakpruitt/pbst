package com.zakpruitt.collectingwithzak.controller;

import com.zakpruitt.collectingwithzak.dto.response.PokemonCardResponse;
import com.zakpruitt.collectingwithzak.dto.response.SealedProductResponse;
import com.zakpruitt.collectingwithzak.service.render.SearchRenderService;
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

    private final SearchRenderService searchRenderService;

    @GetMapping("/cards/search")
    public List<PokemonCardResponse> searchCards(@RequestParam(defaultValue = "") String q) {
        return searchRenderService.searchCards(q);
    }

    @GetMapping("/sealed/search")
    public List<SealedProductResponse> searchSealed(@RequestParam(defaultValue = "") String q) {
        return searchRenderService.searchSealed(q);
    }
}
