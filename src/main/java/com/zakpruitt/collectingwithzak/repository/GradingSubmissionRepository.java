package com.zakpruitt.collectingwithzak.repository;

import com.zakpruitt.collectingwithzak.dto.common.LabeledStat;
import com.zakpruitt.collectingwithzak.entity.GradingSubmission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GradingSubmissionRepository extends JpaRepository<GradingSubmission, Long> {

    @EntityGraph(attributePaths = {"items"})
    List<GradingSubmission> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"items"})
    Optional<GradingSubmission> findWithItemsById(Long id);

    long countByCompany(String company);

    @Query("SELECT new com.zakpruitt.collectingwithzak.dto.common.LabeledStat(g.status, COUNT(g)) " +
            "FROM GradingSubmission g GROUP BY g.status")
    List<LabeledStat> countByStatus();
}
