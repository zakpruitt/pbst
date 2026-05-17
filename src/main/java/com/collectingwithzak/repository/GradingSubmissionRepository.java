package com.collectingwithzak.repository;

import com.collectingwithzak.dto.response.StatusCount;
import com.collectingwithzak.entity.GradingSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GradingSubmissionRepository extends JpaRepository<GradingSubmission, Long> {

    // ---------- Business logic ----------

    @Query("SELECT DISTINCT g FROM GradingSubmission g LEFT JOIN FETCH g.items ORDER BY g.createdAt DESC")
    List<GradingSubmission> findAllWithItemsOrderByCreatedAtDesc();

    @Query("SELECT g FROM GradingSubmission g LEFT JOIN FETCH g.items WHERE g.id = :id")
    Optional<GradingSubmission> findByIdWithItems(Long id);

    long countByCompany(String company);

    @Modifying
    @Query("UPDATE GradingSubmission g SET g.status = :status WHERE g.id = :id")
    void updateStatus(Long id, String status);

    @Modifying
    @Query("UPDATE GradingSubmission g SET g.sendDate = :date WHERE g.id = :id")
    void setSendDate(Long id, LocalDate date);

    @Modifying
    @Query("UPDATE GradingSubmission g SET g.upchargeTotal = :upchargeTotal, g.returnDate = :returnDate WHERE g.id = :id")
    void updateReturnDetails(Long id, double upchargeTotal, LocalDate returnDate);

    // ---------- Dashboard / KPI ----------

    @Query("SELECT new com.collectingwithzak.dto.response.StatusCount(g.status, COUNT(g)) FROM GradingSubmission g GROUP BY g.status")
    List<StatusCount> countByStatus();
}
