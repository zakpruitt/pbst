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

type SaleRepository interface {
	CreateSale(ctx context.Context, sale *models.Sale) error
	Upsert(ctx context.Context, sales []models.Sale) error
	GetByID(ctx context.Context, id uint) (*models.Sale, error)
	GetAllSales(ctx context.Context, view string) ([]models.Sale, error)
	GetByStatus(ctx context.Context, status string) ([]models.Sale, error)
	GetTopByNet(ctx context.Context, limit int) ([]models.Sale, error)
	GetRecent(ctx context.Context, limit int) ([]models.Sale, error)
	CountByStatus(ctx context.Context, status string) (int64, error)
	GetTotalGrossAmount(ctx context.Context) (float64, error)
	GetTotalNetAmount(ctx context.Context) (float64, error)
	GetTotalFees(ctx context.Context) (float64, error)
	VinceTotals(ctx context.Context) (RangeTotals, error)
	TotalsSince(ctx context.Context, since time.Time) (RangeTotals, error)
	MonthlyRevenue(ctx context.Context, months int) ([]MonthlyRevenue, error)
	CountByOrigin(ctx context.Context) ([]OriginCount, error)
	UpdateStatus(ctx context.Context, id uint, status string) error
	UpdateStatusAndAttribution(ctx context.Context, id uint, status, attributedTo string) error
	Delete(ctx context.Context, id uint) error
}

type saleRepository struct {
	db *gorm.DB
}

func NewSaleRepository(db *gorm.DB) SaleRepository {
	return &saleRepository{db: db}
}

// Sale list views. STAGED sales are never shown here — they belong to the
// staging page. Vince's sales live under IGNORED with attributed_to='vince'
// and are filtered out of the "Ignored" view, so each tab is a disjoint set.
const (
	SaleViewMine    = "mine"
	SaleViewIgnored = "ignored"
	SaleViewVince   = "vince"
)

type RangeTotals struct {
	Count int64
	Gross float64
	Net   float64
}

type MonthlyRevenue struct {
	Month string
	Gross float64
	Net   float64
	Count int64
}

type OriginCount struct {
	Origin string
	Count  int64
	Net    float64
}

func (r *saleRepository) CreateSale(ctx context.Context, sale *models.Sale) error {
	err := r.db.WithContext(ctx).Create(sale).Error
	if err != nil {
		return fmt.Errorf("create sale: %w", err)
	}
	return nil
}

// Upsert inserts or updates sales by ebay_order_id. On re-sync, only
// eBay-sourced fields refresh; user-owned fields (status, origin, notes) are
// preserved so triage decisions survive across syncs.
func (r *saleRepository) Upsert(ctx context.Context, sales []models.Sale) error {
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

func (r *saleRepository) GetByID(ctx context.Context, id uint) (*models.Sale, error) {
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

func (r *saleRepository) GetAllSales(ctx context.Context, view string) ([]models.Sale, error) {
	var sales []models.Sale
	q := r.db.WithContext(ctx).Order("sale_date DESC")
	switch view {
	case SaleViewVince:
		q = q.Where("status = ? AND attributed_to = ?", "IGNORED", "vince")
	case SaleViewIgnored:
		q = q.Where("status = ? AND attributed_to != ?", "IGNORED", "vince")
	default:
		q = q.Where("status = ?", "CONFIRMED")
	}
	if err := q.Find(&sales).Error; err != nil {
		return nil, fmt.Errorf("get all sales: %w", err)
	}
	return sales, nil
}

func (r *saleRepository) GetByStatus(ctx context.Context, status string) ([]models.Sale, error) {
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

func (r *saleRepository) GetTopByNet(ctx context.Context, limit int) ([]models.Sale, error) {
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

func (r *saleRepository) GetRecent(ctx context.Context, limit int) ([]models.Sale, error) {
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

func (r *saleRepository) CountByStatus(ctx context.Context, status string) (int64, error) {
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

func (r *saleRepository) GetTotalGrossAmount(ctx context.Context) (float64, error) {
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

func (r *saleRepository) GetTotalNetAmount(ctx context.Context) (float64, error) {
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

func (r *saleRepository) GetTotalFees(ctx context.Context) (float64, error) {
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

func (r *saleRepository) VinceTotals(ctx context.Context) (RangeTotals, error) {
	var totals RangeTotals
	err := r.db.WithContext(ctx).
		Model(&models.Sale{}).
		Select("COUNT(*) AS count, COALESCE(SUM(gross_amount), 0) AS gross, COALESCE(SUM(net_amount), 0) AS net").
		Where("attributed_to = ?", "vince").
		Scan(&totals).Error
	if err != nil {
		return totals, fmt.Errorf("vince totals: %w", err)
	}
	return totals, nil
}

func (r *saleRepository) TotalsSince(ctx context.Context, since time.Time) (RangeTotals, error) {
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

// MonthlyRevenue returns revenue grouped by year-month for the last `months` months.
// Missing months are not filled — the caller should zero-fill the timeline.
func (r *saleRepository) MonthlyRevenue(ctx context.Context, months int) ([]MonthlyRevenue, error) {
	var rows []MonthlyRevenue
	err := r.db.WithContext(ctx).
		Model(&models.Sale{}).
		Select("TO_CHAR(DATE_TRUNC('month', sale_date), 'YYYY-MM') AS month, "+
			"COALESCE(SUM(gross_amount), 0) AS gross, "+
			"COALESCE(SUM(net_amount), 0) AS net, "+
			"COUNT(*) AS count").
		Where("status = ?", "CONFIRMED").
		Where("sale_date >= NOW() - make_interval(months => ?)", months).
		Group("month").
		Order("month").
		Scan(&rows).Error
	if err != nil {
		return nil, fmt.Errorf("monthly revenue: %w", err)
	}
	return rows, nil
}

func (r *saleRepository) CountByOrigin(ctx context.Context) ([]OriginCount, error) {
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

func (r *saleRepository) UpdateStatus(ctx context.Context, id uint, status string) error {
	err := r.db.WithContext(ctx).
		Model(&models.Sale{}).
		Where("id = ?", id).
		Update("status", status).Error
	if err != nil {
		return fmt.Errorf("update sale status: %w", err)
	}
	return nil
}

// UpdateStatusAndAttribution sets both fields in one write so a sale can be
// moved to IGNORED and assigned to Vince (or cleared) atomically.
func (r *saleRepository) UpdateStatusAndAttribution(ctx context.Context, id uint, status, attributedTo string) error {
	err := r.db.WithContext(ctx).
		Model(&models.Sale{}).
		Where("id = ?", id).
		Updates(map[string]any{"status": status, "attributed_to": attributedTo}).Error
	if err != nil {
		return fmt.Errorf("update sale status/attribution: %w", err)
	}
	return nil
}

func (r *saleRepository) Delete(ctx context.Context, id uint) error {
	err := r.db.WithContext(ctx).Delete(&models.Sale{}, id).Error
	if err != nil {
		return fmt.Errorf("delete sale: %w", err)
	}
	return nil
}
