package com.zakpruitt.collectingwithzak.repository;

import com.zakpruitt.collectingwithzak.entity.GradingSubmission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GradingSubmissionRepository extends JpaRepository<GradingSubmission, Long> {

    // open-in-view is off: fetch everything the detail template renders per item.
    @EntityGraph(attributePaths = {"items", "items.pokemonCard", "items.sealedProduct"})
    Optional<GradingSubmission> findWithItemsById(Long id);

    @EntityGraph(attributePaths = {"items"})
    List<GradingSubmission> findAllByOrderByCreatedAtDesc();

    long countByCompany(String company);

    @Query("SELECT new com.zakpruitt.collectingwithzak.repository.LabeledStat(g.status, COUNT(g)) " +
            "FROM GradingSubmission g GROUP BY g.status")
    List<LabeledStat> countByStatus();
}
