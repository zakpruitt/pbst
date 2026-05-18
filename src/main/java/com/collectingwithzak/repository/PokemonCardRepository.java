package com.collectingwithzak.repository;

import com.collectingwithzak.entity.PokemonCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PokemonCardRepository extends JpaRepository<PokemonCard, String>, JpaSpecificationExecutor<PokemonCard> {
}
