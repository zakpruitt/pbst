package com.zakpruitt.pbst.controllers;

import com.zakpruitt.pbst.services.PokeWalletSyncService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
@AllArgsConstructor
public class SyncController {

    private final PokeWalletSyncService pokeWalletSyncService;

    @PostMapping("/group1")
    public ResponseEntity<String> syncGroup1() {
        new Thread(pokeWalletSyncService::syncGroup1Sets).start(); // Run async
        return ResponseEntity.ok("Sync started for Group 1");
    }
}
