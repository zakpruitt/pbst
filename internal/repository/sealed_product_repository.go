package repository

import (
	"context"
	"fmt"

	"github.com/zakpruitt/pbst/internal/models"
	"gorm.io/gorm"
)

type SealedProductRepository struct {
	db *gorm.DB
}

func NewSealedProductRepository(db *gorm.DB) *SealedProductRepository {
	return &SealedProductRepository{db: db}
}

func (r *SealedProductRepository) Search(ctx context.Context, query string) ([]models.SealedProduct, error) {
	var products []models.SealedProduct
	like := "%" + query + "%"
	err := r.db.WithContext(ctx).
		Where("name ILIKE ? OR set_name ILIKE ?", like, like).
		Limit(10).
		Find(&products).Error
	if err != nil {
		return nil, fmt.Errorf("search sealed products: %w", err)
	}
	return products, nil
}
