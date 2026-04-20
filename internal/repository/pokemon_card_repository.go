package repository

import (
	"context"
	"fmt"

	"github.com/zakpruitt/pbst/internal/models"
	"gorm.io/gorm"
	"gorm.io/gorm/clause"
)

type PokemonCardRepository interface {
	Upsert(ctx context.Context, cards []models.PokemonCard) error
	Search(ctx context.Context, query string) ([]models.PokemonCard, error)
}

type pokemonCardRepository struct {
	db *gorm.DB
}

func NewPokemonCardRepository(db *gorm.DB) PokemonCardRepository {
	return &pokemonCardRepository{db: db}
}

func (r *pokemonCardRepository) Upsert(ctx context.Context, cards []models.PokemonCard) error {
	err := r.db.WithContext(ctx).Clauses(clause.OnConflict{UpdateAll: true}).Create(&cards).Error
	if err != nil {
		return fmt.Errorf("upsert cards: %w", err)
	}
	return nil
}

func (r *pokemonCardRepository) Search(ctx context.Context, query string) ([]models.PokemonCard, error) {
	return searchByName[models.PokemonCard](ctx, r.db, query, 10)
}
