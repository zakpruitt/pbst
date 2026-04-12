package repository

import (
	"context"
	"fmt"

	"github.com/zakpruitt/pbst/internal/models"
	"gorm.io/gorm"
	"gorm.io/gorm/clause"
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
		return fmt.Errorf("create sale: %w", err)
	}
	return nil
}

// Upsert inserts or updates sales by ebay_order_id.
// Re-running the sync will update existing rows rather than duplicating them.
func (r *SaleRepository) Upsert(ctx context.Context, sales []models.Sale) error {
	if len(sales) == 0 {
		return nil
	}
	err := r.db.WithContext(ctx).
		Clauses(clause.OnConflict{
			Columns: []clause.Column{{Name: "ebay_order_id"}},
			DoUpdates: clause.AssignmentColumns([]string{
				"sale_date", "title", "buyer_username", "gross_amount",
				"ebay_fees", "shipping_cost", "net_amount", "order_status", "updated_at",
			}),
		}).
		Create(&sales).Error
	if err != nil {
		return fmt.Errorf("upsert sales: %w", err)
	}
	return nil
}

func (r *SaleRepository) GetAllSales(ctx context.Context) ([]models.Sale, error) {
	var sales []models.Sale
	err := r.db.WithContext(ctx).Find(&sales).Error
	if err != nil {
		return nil, fmt.Errorf("get all sales: %w", err)
	}
	return sales, nil
}

func (r *SaleRepository) GetTotalGrossAmount(ctx context.Context) (float64, error) {
	var total float64
	err := r.db.WithContext(ctx).
		Model(&models.Sale{}).
		Select("COALESCE(SUM(gross_amount), 0)").
		Scan(&total).Error
	if err != nil {
		return 0, fmt.Errorf("get total gross: %w", err)
	}
	return total, nil
}

func (r *SaleRepository) GetTotalNetAmount(ctx context.Context) (float64, error) {
	var total float64
	err := r.db.WithContext(ctx).
		Model(&models.Sale{}).
		Select("COALESCE(SUM(net_amount), 0)").
		Scan(&total).Error
	if err != nil {
		return 0, fmt.Errorf("get total net: %w", err)
	}
	return total, nil
}
