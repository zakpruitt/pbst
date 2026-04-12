package repository

import (
	"context"
	"fmt"

	"github.com/zakpruitt/pbst/internal/models"
	"gorm.io/gorm"
	"gorm.io/gorm/clause"
)

type PokemonCardRepository struct {
	db *gorm.DB
}

func NewPokemonCardRepository(db *gorm.DB) *PokemonCardRepository {
	return &PokemonCardRepository{db: db}
}

// Upsert inserts or updates cards by primary key. Used during set syncs.
func (r *PokemonCardRepository) Upsert(ctx context.Context, cards []models.PokemonCard) error {
	err := r.db.WithContext(ctx).Clauses(clause.OnConflict{UpdateAll: true}).Create(&cards).Error
	if err != nil {
		return fmt.Errorf("upsert cards: %w", err)
	}
	return nil
}
func (r *PokemonCardRepository) Search(ctx context.Context, query string) ([]models.PokemonCard, error) {
	var cards []models.PokemonCard
	like := "%" + query + "%"
	err := r.db.WithContext(ctx).
		Where("name ILIKE ? OR set_name ILIKE ?", like, like).
		Limit(10).
		Find(&cards).Error
	if err != nil {
		return nil, fmt.Errorf("search cards: %w", err)
	}
	return cards, nil
}
