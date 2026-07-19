package com.zakpruitt.collectingwithzak.dto.render;

import com.zakpruitt.collectingwithzak.dto.common.MonthGroup;
import com.zakpruitt.collectingwithzak.dto.common.VinceLedger;
import com.zakpruitt.collectingwithzak.entity.Sale;
import com.zakpruitt.collectingwithzak.entity.VincePayment;

import java.util.List;

public record SaleIndexData(
        List<MonthGroup<Sale>> groups,
        long stagedCount,
        String view,
        VinceLedger vinceLedger,
        List<MonthGroup<VincePayment>> vincePaymentGroups) {
}
