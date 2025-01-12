package com.zakpruitt.pbst.repositories;

import com.zakpruitt.pbst.entities.GradingSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface GradingSubmissionRepository extends JpaRepository<GradingSubmission, Long> {

    @Query("SELECT SUM(g.totalGradingCost) FROM GradingSubmission g")
    BigDecimal sumTotalGradingCost();
}
