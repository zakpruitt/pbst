package com.collectingwithzak.repository;

import com.collectingwithzak.entity.VincePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VincePaymentRepository extends JpaRepository<VincePayment, Long> {

    List<VincePayment> findAllByOrderByPaymentDateDescIdDesc();

    @Query(value = "SELECT COALESCE(SUM(CASE WHEN type = 'PAYOUT' THEN amount ELSE 0 END), 0), " +
                   "COALESCE(SUM(CASE WHEN type = 'RECEIVABLE' THEN amount ELSE 0 END), 0) " +
                   "FROM vince_payments WHERE deleted_at IS NULL", nativeQuery = true)
    List<Object[]> getTotalsRaw();

    default double[] getTotals() {
        Object[] row = getTotalsRaw().getFirst();
        return new double[]{((Number) row[0]).doubleValue(), ((Number) row[1]).doubleValue()};
    }
}
