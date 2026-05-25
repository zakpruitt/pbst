package com.zakpruitt.collectingwithzak.repository;

import com.zakpruitt.collectingwithzak.dto.common.LabeledStat;
import com.zakpruitt.collectingwithzak.dto.common.RangeTotals;
import com.zakpruitt.collectingwithzak.entity.Sale;
import com.zakpruitt.collectingwithzak.entity.enums.SaleStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    Sale findByEbayOrderId(String ebayOrderId);

    @EntityGraph(attributePaths = {"items"})
    Optional<Sale> findWithItemsById(Long id);

    List<Sale> findByStatusOrderBySaleDateDesc(SaleStatus status);

    List<Sale> findByStatusOrderBySaleDateDesc(SaleStatus status, Pageable pageable);

    List<Sale> findByStatusOrderByNetAmountDesc(SaleStatus status, Pageable pageable);

    List<Sale> findByStatusAndAttributedToOrderBySaleDateDesc(SaleStatus status, String attributedTo);

    @Query("SELECT s FROM Sale s WHERE s.status = 'IGNORED' " +
            "AND (s.attributedTo IS NULL OR s.attributedTo = '') ORDER BY s.saleDate DESC")
    List<Sale> findIgnored();

    long countByStatus(SaleStatus status);

    @Query("SELECT new com.zakpruitt.collectingwithzak.dto.common.RangeTotals(" +
            "COUNT(s), COALESCE(SUM(s.grossAmount), 0.0), COALESCE(SUM(s.netAmount), 0.0), " +
            "COALESCE(SUM(s.ebayFees) + SUM(s.shippingCost), 0.0)) " +
            "FROM Sale s WHERE s.status = 'CONFIRMED'")
    RangeTotals getConfirmedTotals();

    @Query("SELECT new com.zakpruitt.collectingwithzak.dto.common.RangeTotals(" +
            "COUNT(s), COALESCE(SUM(s.grossAmount), 0.0), COALESCE(SUM(s.netAmount), 0.0), 0.0) " +
            "FROM Sale s WHERE s.attributedTo = 'vince'")
    RangeTotals getVinceTotals();

    @Query("SELECT new com.zakpruitt.collectingwithzak.dto.common.RangeTotals(" +
            "COUNT(s), COALESCE(SUM(s.grossAmount), 0.0), COALESCE(SUM(s.netAmount), 0.0), 0.0) " +
            "FROM Sale s WHERE s.status = 'CONFIRMED' AND s.saleDate >= :since")
    RangeTotals getTotalsSince(LocalDate since);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('month', sale_date), 'YYYY-MM') AS month, " +
            "COALESCE(SUM(gross_amount), 0) AS gross, COALESCE(SUM(net_amount), 0) AS net " +
            "FROM sales s WHERE s.status = 'CONFIRMED' " +
            "AND s.sale_date >= NOW() - make_interval(months => :months) " +
            "GROUP BY month ORDER BY month", nativeQuery = true)
    List<Object[]> getMonthlyRevenueRaw(int months);

    @Query("SELECT new com.zakpruitt.collectingwithzak.dto.common.LabeledStat(s.origin, COUNT(s)) " +
            "FROM Sale s WHERE s.status = 'CONFIRMED' GROUP BY s.origin")
    List<LabeledStat> countByOrigin();

}
