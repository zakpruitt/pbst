package com.collectingwithzak.repository;

import com.collectingwithzak.dto.common.LabeledStat;
import com.collectingwithzak.entity.GradingSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GradingSubmissionRepository extends JpaRepository<GradingSubmission, Long> {
    @Query("SELECT DISTINCT g FROM GradingSubmission g LEFT JOIN FETCH g.items ORDER BY g.createdAt DESC")
    List<GradingSubmission> findAllWithItems();

    @Query("SELECT g FROM GradingSubmission g LEFT JOIN FETCH g.items WHERE g.id = :id")
    Optional<GradingSubmission> findByIdWithItems(Long id);

    long countByCompany(String company);

    @Query("SELECT new com.collectingwithzak.dto.common.LabeledStat(g.status, COUNT(g)) FROM GradingSubmission g GROUP BY g.status")
    List<LabeledStat> countByStatus();
}
