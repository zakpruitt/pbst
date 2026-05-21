package com.collectingwithzak.dto.page;

import com.collectingwithzak.dto.common.MonthGroup;
import com.collectingwithzak.dto.response.SaleResponse;
import com.collectingwithzak.dto.response.VinceLedger;
import com.collectingwithzak.dto.response.VincePaymentResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleIndexData {
    private List<MonthGroup<SaleResponse>> groups;
    private long stagedCount;
    private String view;
    private VinceLedger vinceLedger;
    private List<MonthGroup<VincePaymentResponse>> vincePaymentGroups;
}
