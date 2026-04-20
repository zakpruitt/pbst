package com.collectingwithzak.repository;

import com.collectingwithzak.dto.response.MonthlyRevenue;
import com.collectingwithzak.dto.response.OriginCount;
import com.collectingwithzak.dto.response.RangeTotals;
import com.collectingwithzak.entity.Sale;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findByStatusOrderBySaleDateDesc(String status);

    List<Sale> findByStatusAndAttributedToOrderBySaleDateDesc(String status, String attributedTo);

    @Query("SELECT s FROM Sale s WHERE s.status = 'IGNORED' AND (s.attributedTo IS NULL OR s.attributedTo = '') ORDER BY s.saleDate DESC")
    List<Sale> findIgnored();

    long countByStatus(String status);

    Sale findByEbayOrderId(String ebayOrderId);

    @Query("SELECT s FROM Sale s WHERE s.status = 'CONFIRMED' ORDER BY s.netAmount DESC")
    List<Sale> findTopByNet(Pageable pageable);

    @Query("SELECT s FROM Sale s WHERE s.status = 'CONFIRMED' ORDER BY s.saleDate DESC")
    List<Sale> findRecent(Pageable pageable);

    @Query("SELECT COALESCE(SUM(s.grossAmount), 0) FROM Sale s WHERE s.status = 'CONFIRMED'")
    double getTotalGross();

    @Query("SELECT COALESCE(SUM(s.netAmount), 0) FROM Sale s WHERE s.status = 'CONFIRMED'")
    double getTotalNet();

    @Query("SELECT COALESCE(SUM(s.ebayFees + s.shippingCost), 0) FROM Sale s WHERE s.status = 'CONFIRMED'")
    double getTotalFees();

    @Query(value = "SELECT COUNT(*), COALESCE(SUM(gross_amount), 0), COALESCE(SUM(net_amount), 0) " +
                   "FROM sales WHERE attributed_to = 'vince'", nativeQuery = true)
    Object[] getVinceTotalsRaw();

    @Query(value = "SELECT COUNT(*), COALESCE(SUM(gross_amount), 0), COALESCE(SUM(net_amount), 0) " +
                   "FROM sales WHERE status = 'CONFIRMED' AND sale_date >= :since", nativeQuery = true)
    Object[] getTotalsSinceRaw(LocalDate since);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('month', sale_date), 'YYYY-MM') AS month, " +
                   "COALESCE(SUM(gross_amount), 0) AS gross, COALESCE(SUM(net_amount), 0) AS net, " +
                   "COUNT(*) AS count FROM sales WHERE status = 'CONFIRMED' " +
                   "AND sale_date >= NOW() - make_interval(months => :months) " +
                   "GROUP BY month ORDER BY month", nativeQuery = true)
    List<Object[]> getMonthlyRevenueRaw(int months);

    @Query(value = "SELECT origin, COUNT(*), COALESCE(SUM(net_amount), 0) " +
                   "FROM sales WHERE status = 'CONFIRMED' GROUP BY origin", nativeQuery = true)
    List<Object[]> countByOriginRaw();

    @Modifying
    @Query("UPDATE Sale s SET s.status = :status WHERE s.id = :id")
    void updateStatus(Long id, String status);

    @Modifying
    @Query("UPDATE Sale s SET s.status = :status, s.attributedTo = :attributedTo WHERE s.id = :id")
    void updateStatusAndAttribution(Long id, String status, String attributedTo);

    default List<Sale> findConfirmed() {
        return findByStatusOrderBySaleDateDesc("CONFIRMED");
    }

    default List<Sale> findVince() {
        return findByStatusAndAttributedToOrderBySaleDateDesc("IGNORED", "vince");
    }

    default RangeTotals getVinceTotals() {
        Object[] row = getVinceTotalsRaw();
        return new RangeTotals(((Number) row[0]).longValue(), ((Number) row[1]).doubleValue(), ((Number) row[2]).doubleValue());
    }

    default RangeTotals getTotalsSince(LocalDate since) {
        Object[] row = getTotalsSinceRaw(since);
        return new RangeTotals(((Number) row[0]).longValue(), ((Number) row[1]).doubleValue(), ((Number) row[2]).doubleValue());
    }

    default List<MonthlyRevenue> getMonthlyRevenue(int months) {
        return getMonthlyRevenueRaw(months).stream()
                .map(row -> new MonthlyRevenue(
                        (String) row[0],
                        ((Number) row[1]).doubleValue(),
                        ((Number) row[2]).doubleValue(),
                        ((Number) row[3]).longValue()))
                .toList();
    }

    default List<OriginCount> countByOrigin() {
        return countByOriginRaw().stream()
                .map(row -> new OriginCount((String) row[0], ((Number) row[1]).longValue(), ((Number) row[2]).doubleValue()))
                .toList();
    }
}
