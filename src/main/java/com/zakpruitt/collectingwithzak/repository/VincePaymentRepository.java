package com.zakpruitt.collectingwithzak.repository;

import com.zakpruitt.collectingwithzak.dto.common.VincePaymentTotals;
import com.zakpruitt.collectingwithzak.entity.VincePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VincePaymentRepository extends JpaRepository<VincePayment, Long> {

    List<VincePayment> findAllByOrderByPaymentDateDescIdDesc();

    @Query("SELECT new com.zakpruitt.collectingwithzak.dto.common.VincePaymentTotals(" +
            "COALESCE(SUM(CASE WHEN v.type = 'PAYOUT' THEN v.amount ELSE 0.0 END), 0.0), " +
            "COALESCE(SUM(CASE WHEN v.type = 'RECEIVABLE' THEN v.amount ELSE 0.0 END), 0.0)) " +
            "FROM VincePayment v")
    VincePaymentTotals getTotals();
}
