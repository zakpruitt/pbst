package repository

import (
	"context"
	"fmt"

	"github.com/zakpruitt/pbst/internal/models"
	"gorm.io/gorm"
)

type SaleRepository struct {
	db *gorm.DB
}

func NewSaleRepository(db *gorm.DB) *SaleRepository {
	return &SaleRepository{db: db}
}

func (r *SaleRepository) CreateSale(ctx context.Context, sale *models.Sale) error {
	err := r.db.WithContext(ctx).Create(sale).Error
	if err != nil {
		return fmt.Errorf("CreateSale: %w", err)
	}
	return nil
}

func (r *SaleRepository) GetAllSales(ctx context.Context) ([]models.Sale, error) {
	var sales []models.Sale
	err := r.db.WithContext(ctx).Find(&sales).Error
	if err != nil {
		return nil, fmt.Errorf("GetAllSales: %w", err)
	}
	return sales, nil
}

func (r *SaleRepository) GetTotalNetAmount(ctx context.Context) (float64, error) {
	var total float64
	err := r.db.WithContext(ctx).Model(&models.Sale{}).
		Select("COALESCE(SUM(net_amount), 0)").
		Scan(&total).Error
	if err != nil {
		return 0, fmt.Errorf("GetTotalNetAmount: %w", err)
	}
	return total, nil
}

func (r *SaleRepository) GetTotalGrossAmount(ctx context.Context) (float64, error) {
	var total float64
	err := r.db.WithContext(ctx).Model(&models.Sale{}).
		Select("COALESCE(SUM(gross_amount), 0)").
		Scan(&total).Error
	if err != nil {
		return 0, fmt.Errorf("GetTotalGrossAmount: %w", err)
	}
	return total, nil
}
