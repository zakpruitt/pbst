package com.zakpruitt.pbst.repositories;

import com.zakpruitt.pbst.entities.PokemonCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PokemonCardRepository extends JpaRepository<PokemonCard, String> {

    List<PokemonCard> findBySetCodeOrderByCardNumberAsc(String setCode);

    List<PokemonCard> findByNameContainingIgnoreCase(String name);

    List<PokemonCard> findBySetCodeIn(List<String> setCodes);
}