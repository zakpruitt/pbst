package repository

import (
	"context"
	"errors"
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

// Upsert inserts or updates sales by ebay_order_id. On re-sync, only
// eBay-sourced fields (amounts, dates, fulfillment status) refresh; user-owned
// fields (status, origin, notes, image_url) are preserved so triage decisions
// and manual edits survive across syncs.
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

// GetAllSales returns CONFIRMED sales ordered newest first. When includeIgnored
// is true, IGNORED sales are also included. STAGED sales are never shown here
// — they belong to the staging page.
func (r *SaleRepository) GetAllSales(ctx context.Context, includeIgnored bool) ([]models.Sale, error) {
	var sales []models.Sale
	q := r.db.WithContext(ctx).Order("sale_date DESC")
	if includeIgnored {
		q = q.Where("status IN ?", []string{"CONFIRMED", "IGNORED"})
	} else {
		q = q.Where("status = ?", "CONFIRMED")
	}
	if err := q.Find(&sales).Error; err != nil {
		return nil, fmt.Errorf("get all sales: %w", err)
	}
	return sales, nil
}

func (r *SaleRepository) GetByID(ctx context.Context, id uint) (*models.Sale, error) {
	var sale models.Sale
	err := r.db.WithContext(ctx).
		Preload("Items.PokemonCard").
		Preload("Items.SealedProduct").
		First(&sale, id).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return nil, err
	}
	if err != nil {
		return nil, fmt.Errorf("get sale: %w", err)
	}
	return &sale, nil
}

func (r *SaleRepository) GetByStatus(ctx context.Context, status string) ([]models.Sale, error) {
	var sales []models.Sale
	err := r.db.WithContext(ctx).
		Where("status = ?", status).
		Order("sale_date DESC").
		Find(&sales).Error
	if err != nil {
		return nil, fmt.Errorf("get sales by status: %w", err)
	}
	return sales, nil
}

func (r *SaleRepository) UpdateStatus(ctx context.Context, id uint, status string) error {
	err := r.db.WithContext(ctx).
		Model(&models.Sale{}).
		Where("id = ?", id).
		Update("status", status).Error
	if err != nil {
		return fmt.Errorf("update sale status: %w", err)
	}
	return nil
}

func (r *SaleRepository) Delete(ctx context.Context, id uint) error {
	err := r.db.WithContext(ctx).Delete(&models.Sale{}, id).Error
	if err != nil {
		return fmt.Errorf("delete sale: %w", err)
	}
	return nil
}

func (r *SaleRepository) CountByStatus(ctx context.Context, status string) (int64, error) {
	var count int64
	err := r.db.WithContext(ctx).
		Model(&models.Sale{}).
		Where("status = ?", status).
		Count(&count).Error
	if err != nil {
		return 0, fmt.Errorf("count sales by status: %w", err)
	}
	return count, nil
}

func (r *SaleRepository) GetTotalGrossAmount(ctx context.Context) (float64, error) {
	var total float64
	err := r.db.WithContext(ctx).
		Model(&models.Sale{}).
		Where("status = ?", "CONFIRMED").
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
		Where("status = ?", "CONFIRMED").
		Select("COALESCE(SUM(net_amount), 0)").
		Scan(&total).Error
	if err != nil {
		return 0, fmt.Errorf("get total net: %w", err)
	}
	return total, nil
}
