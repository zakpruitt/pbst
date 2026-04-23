package com.collectingwithzak.controller;

import com.collectingwithzak.dto.response.CardSearchResult;
import com.collectingwithzak.dto.response.SealedSearchResult;
import com.collectingwithzak.service.SearchService;
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
    public List<CardSearchResult> searchCards(@RequestParam(defaultValue = "") String q) {
        return searchService.searchCards(q);
    }

    @GetMapping("/sealed/search")
    public List<SealedSearchResult> searchSealed(@RequestParam(defaultValue = "") String q) {
        return searchService.searchSealed(q);
    }
}
