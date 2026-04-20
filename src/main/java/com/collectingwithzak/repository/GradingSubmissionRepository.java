package com.collectingwithzak.repository;

import com.collectingwithzak.dto.response.GradingStatusCount;
import com.collectingwithzak.entity.GradingSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface GradingSubmissionRepository extends JpaRepository<GradingSubmission, Long> {

    List<GradingSubmission> findAllByOrderByCreatedAtDesc();

    long countByCompany(String company);

    @Query(value = "SELECT status, COUNT(*) FROM grading_submissions GROUP BY status", nativeQuery = true)
    List<Object[]> countByStatusRaw();

    @Modifying
    @Query("UPDATE GradingSubmission g SET g.status = :status WHERE g.id = :id")
    void updateStatus(Long id, String status);

    @Modifying
    @Query("UPDATE GradingSubmission g SET g.sendDate = :date WHERE g.id = :id")
    void setSendDate(Long id, LocalDate date);

    @Modifying
    @Query("UPDATE GradingSubmission g SET g.upchargeTotal = :upchargeTotal, g.returnDate = :returnDate WHERE g.id = :id")
    void updateReturnDetails(Long id, double upchargeTotal, LocalDate returnDate);

    default List<GradingStatusCount> countByStatus() {
        return countByStatusRaw().stream()
                .map(row -> new GradingStatusCount((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }
}
