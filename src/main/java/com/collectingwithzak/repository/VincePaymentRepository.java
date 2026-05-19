package com.collectingwithzak.repository;

import com.collectingwithzak.dto.vince.VincePaymentTotals;
import com.collectingwithzak.entity.VincePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VincePaymentRepository extends JpaRepository<VincePayment, Long> {

    List<VincePayment> findAllByOrderByPaymentDateDescIdDesc();

    @Query(value = "SELECT COALESCE(SUM(CASE WHEN type = 'PAYOUT' THEN amount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN type = 'RECEIVABLE' THEN amount ELSE 0 END), 0) " +
            "FROM vince_payments", nativeQuery = true)
    List<Object[]> getTotalsRaw();

    default VincePaymentTotals getTotals() {
        List<Object[]> results = getTotalsRaw();
        if (results.isEmpty()) return new VincePaymentTotals(0, 0);
        Object[] row = results.getFirst();
        return new VincePaymentTotals(
                ((Number) row[0]).doubleValue(),
                ((Number) row[1]).doubleValue());
    }
}
