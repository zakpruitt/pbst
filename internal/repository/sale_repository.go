package repository

import (
	"context"
	"errors"
	"fmt"
	"time"

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

func (r *SaleRepository) GetTotalFees(ctx context.Context) (float64, error) {
	var total float64
	err := r.db.WithContext(ctx).
		Model(&models.Sale{}).
		Where("status = ?", "CONFIRMED").
		Select("COALESCE(SUM(ebay_fees + shipping_cost), 0)").
		Scan(&total).Error
	if err != nil {
		return 0, fmt.Errorf("get total fees: %w", err)
	}
	return total, nil
}

type MonthlyRevenue struct {
	Month string
	Gross float64
	Net   float64
	Count int64
}

// MonthlyRevenue returns revenue grouped by year-month for the last `months` months.
// Missing months are not filled — the caller should zero-fill the timeline.
func (r *SaleRepository) MonthlyRevenue(ctx context.Context, months int) ([]MonthlyRevenue, error) {
	var rows []MonthlyRevenue
	err := r.db.WithContext(ctx).
		Model(&models.Sale{}).
		Select("TO_CHAR(DATE_TRUNC('month', sale_date), 'YYYY-MM') AS month, " +
			"COALESCE(SUM(gross_amount), 0) AS gross, " +
			"COALESCE(SUM(net_amount), 0) AS net, " +
			"COUNT(*) AS count").
		Where("status = ?", "CONFIRMED").
		Where("sale_date >= NOW() - (? || ' months')::interval", months).
		Group("month").
		Order("month").
		Scan(&rows).Error
	if err != nil {
		return nil, fmt.Errorf("monthly revenue: %w", err)
	}
	return rows, nil
}

type OriginCount struct {
	Origin string
	Count  int64
	Net    float64
}

func (r *SaleRepository) CountByOrigin(ctx context.Context) ([]OriginCount, error) {
	var rows []OriginCount
	err := r.db.WithContext(ctx).
		Model(&models.Sale{}).
		Select("origin, COUNT(*) AS count, COALESCE(SUM(net_amount), 0) AS net").
		Where("status = ?", "CONFIRMED").
		Group("origin").
		Scan(&rows).Error
	if err != nil {
		return nil, fmt.Errorf("count by origin: %w", err)
	}
	return rows, nil
}

func (r *SaleRepository) GetTopByNet(ctx context.Context, limit int) ([]models.Sale, error) {
	var sales []models.Sale
	err := r.db.WithContext(ctx).
		Where("status = ?", "CONFIRMED").
		Order("net_amount DESC").
		Limit(limit).
		Find(&sales).Error
	if err != nil {
		return nil, fmt.Errorf("get top sales: %w", err)
	}
	return sales, nil
}

func (r *SaleRepository) GetRecent(ctx context.Context, limit int) ([]models.Sale, error) {
	var sales []models.Sale
	err := r.db.WithContext(ctx).
		Where("status = ?", "CONFIRMED").
		Order("sale_date DESC").
		Limit(limit).
		Find(&sales).Error
	if err != nil {
		return nil, fmt.Errorf("get recent sales: %w", err)
	}
	return sales, nil
}

type RangeTotals struct {
	Count int64
	Gross float64
	Net   float64
}

func (r *SaleRepository) TotalsSince(ctx context.Context, since time.Time) (RangeTotals, error) {
	var totals RangeTotals
	err := r.db.WithContext(ctx).
		Model(&models.Sale{}).
		Select("COUNT(*) AS count, COALESCE(SUM(gross_amount), 0) AS gross, COALESCE(SUM(net_amount), 0) AS net").
		Where("status = ?", "CONFIRMED").
		Where("sale_date >= ?", since).
		Scan(&totals).Error
	if err != nil {
		return totals, fmt.Errorf("totals since: %w", err)
	}
	return totals, nil
}
