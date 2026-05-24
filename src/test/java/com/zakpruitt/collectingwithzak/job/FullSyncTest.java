package com.zakpruitt.collectingwithzak.job;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("fullsynclocal")
@Disabled("Manual runner — remove @Disabled to sync all eng/jap sets against a live DB")
class FullSyncTest {

    @Autowired
    private PokeWalletSyncJob syncJob;

    @Test
    void syncAllSets() {
        syncJob.syncAll();
    }
}
