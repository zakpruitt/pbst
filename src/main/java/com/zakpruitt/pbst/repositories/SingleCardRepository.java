package com.zakpruitt.pbst.repositories;

import com.zakpruitt.pbst.entities.SingleCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SingleCardRepository extends JpaRepository<SingleCard, Long> {
}
