package com.zakpruitt.pbst.repositories;

import com.zakpruitt.pbst.entities.SingleCard;
import com.zakpruitt.pbst.enums.GradingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SingleCardRepository extends JpaRepository<SingleCard, Long> {
    List<SingleCard> findBySubmittedForGradingTrue();

    List<SingleCard> findByGradingStatus(GradingStatus gradingStatus);

    List<SingleCard> findBySetName(String setName);
}
