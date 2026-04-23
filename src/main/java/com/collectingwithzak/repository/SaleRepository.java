package com.collectingwithzak.repository;

import com.collectingwithzak.dto.response.MonthlyRevenue;
import com.collectingwithzak.dto.response.OriginCount;
import com.collectingwithzak.dto.response.RangeTotals;
import com.collectingwithzak.entity.Sale;
import com.collectingwithzak.entity.enums.SaleStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    // ---------- Business logic ----------

    Sale findByEbayOrderId(String ebayOrderId);

    @Query("SELECT s FROM Sale s LEFT JOIN FETCH s.items WHERE s.id = :id")
    Optional<Sale> findByIdWithItems(Long id);

    List<Sale> findByStatusOrderBySaleDateDesc(String status);

    List<Sale> findByStatusAndAttributedToOrderBySaleDateDesc(String status, String attributedTo);

    @Query("SELECT s FROM Sale s WHERE s.status = 'IGNORED' AND (s.attributedTo IS NULL OR s.attributedTo = '') ORDER BY s.saleDate DESC")
    List<Sale> findIgnored();

    long countByStatus(String status);

    @Modifying
    @Query("UPDATE Sale s SET s.status = :status WHERE s.id = :id")
    void updateStatus(Long id, String status);

    @Modifying
    @Query("UPDATE Sale s SET s.status = :status, s.attributedTo = :attributedTo WHERE s.id = :id")
    void updateStatusAndAttribution(Long id, String status, String attributedTo);

    default List<Sale> findConfirmed() {
        return findByStatusOrderBySaleDateDesc(SaleStatus.CONFIRMED.name());
    }

    default List<Sale> findVince() {
        return findByStatusAndAttributedToOrderBySaleDateDesc(SaleStatus.IGNORED.name(), "vince");
    }

    // ---------- Dashboard / KPI ----------

    @Query("SELECT s FROM Sale s WHERE s.status = 'CONFIRMED' ORDER BY s.netAmount DESC")
    List<Sale> findTopByNet(Pageable pageable);

    @Query("SELECT s FROM Sale s WHERE s.status = 'CONFIRMED' ORDER BY s.saleDate DESC")
    List<Sale> findRecent(Pageable pageable);

    @Query(value = "SELECT COUNT(*), COALESCE(SUM(gross_amount), 0), COALESCE(SUM(net_amount), 0), " +
                   "COALESCE(SUM(ebay_fees + shipping_cost), 0) " +
                   "FROM sales WHERE status = 'CONFIRMED' AND deleted_at IS NULL", nativeQuery = true)
    List<Object[]> getConfirmedTotalsRaw();

    @Query(value = "SELECT COUNT(*), COALESCE(SUM(gross_amount), 0), COALESCE(SUM(net_amount), 0) " +
                   "FROM sales WHERE attributed_to = 'vince' AND deleted_at IS NULL", nativeQuery = true)
    List<Object[]> getVinceTotalsRaw();

    @Query(value = "SELECT COUNT(*), COALESCE(SUM(gross_amount), 0), COALESCE(SUM(net_amount), 0) " +
                   "FROM sales WHERE status = 'CONFIRMED' AND sale_date >= :since AND deleted_at IS NULL", nativeQuery = true)
    List<Object[]> getTotalsSinceRaw(LocalDate since);

    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('month', sale_date), 'YYYY-MM') AS month, " +
                   "COALESCE(SUM(gross_amount), 0) AS gross, COALESCE(SUM(net_amount), 0) AS net, " +
                   "COUNT(*) AS count FROM sales WHERE status = 'CONFIRMED' AND deleted_at IS NULL " +
                   "AND sale_date >= NOW() - make_interval(months => :months) " +
                   "GROUP BY month ORDER BY month", nativeQuery = true)
    List<Object[]> getMonthlyRevenueRaw(int months);

    @Query(value = "SELECT origin, COUNT(*), COALESCE(SUM(net_amount), 0) " +
                   "FROM sales WHERE status = 'CONFIRMED' AND deleted_at IS NULL GROUP BY origin", nativeQuery = true)
    List<Object[]> countByOriginRaw();

    default double[] getConfirmedTotals() {
        Object[] row = getConfirmedTotalsRaw().getFirst();
        return new double[]{
                ((Number) row[0]).doubleValue(),
                ((Number) row[1]).doubleValue(),
                ((Number) row[2]).doubleValue(),
                ((Number) row[3]).doubleValue()
        };
    }

    default RangeTotals getVinceTotals() {
        Object[] row = getVinceTotalsRaw().getFirst();
        return new RangeTotals(((Number) row[0]).longValue(), ((Number) row[1]).doubleValue(), ((Number) row[2]).doubleValue());
    }

    default RangeTotals getTotalsSince(LocalDate since) {
        Object[] row = getTotalsSinceRaw(since).getFirst();
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
