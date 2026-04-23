package com.collectingwithzak.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VinceLedger {
    private long salesCount;
    private double salesGross;
    private double salesNet;
    private double totalPaidOut;
    private double totalVinceOwes;
    private double balance;

    public static VinceLedger from(RangeTotals salesTotals, double paidOut, double vinceOwes) {
        double balance = salesTotals.getNet() - paidOut - vinceOwes;
        return new VinceLedger(
                salesTotals.getCount(),
                salesTotals.getGross(),
                salesTotals.getNet(),
                paidOut,
                vinceOwes,
                balance
        );
    }
}
