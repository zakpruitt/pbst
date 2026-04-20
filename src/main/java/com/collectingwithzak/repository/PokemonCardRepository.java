package com.collectingwithzak.repository;

import com.collectingwithzak.entity.PokemonCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PokemonCardRepository extends JpaRepository<PokemonCard, String> {

    @Query("SELECT c FROM PokemonCard c " +
           "WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.setName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY c.name LIMIT 10")
    List<PokemonCard> search(String query);
}
