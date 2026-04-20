package repository

import (
	"context"

	"github.com/zakpruitt/pbst/internal/models"
	"gorm.io/gorm"
)

type SealedProductRepository interface {
	Search(ctx context.Context, query string) ([]models.SealedProduct, error)
}

type sealedProductRepository struct {
	db *gorm.DB
}

func NewSealedProductRepository(db *gorm.DB) SealedProductRepository {
	return &sealedProductRepository{db: db}
}

func (r *sealedProductRepository) Search(ctx context.Context, query string) ([]models.SealedProduct, error) {
	return searchByName[models.SealedProduct](ctx, r.db, query, 10)
}
