package com.zakpruitt.collectingwithzak.dto.common;

import com.zakpruitt.collectingwithzak.repository.RangeTotals;
import com.zakpruitt.collectingwithzak.repository.VincePaymentRepository.VincePaymentTotals;

public record VinceLedger(long salesCount, double salesGross, double salesNet,
                          double totalPaidOut, double totalVinceOwes, double balance) {

    public static VinceLedger from(RangeTotals salesTotals, VincePaymentTotals paymentTotals) {
        double balance = salesTotals.net() - paymentTotals.paidOut() - paymentTotals.vinceOwes();
        return new VinceLedger(salesTotals.count(), salesTotals.gross(), salesTotals.net(),
                paymentTotals.paidOut(), paymentTotals.vinceOwes(), balance);
    }
}
