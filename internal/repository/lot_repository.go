package repository

import (
	"context"
	"fmt"

	"github.com/zakpruitt/pbst/internal/models"
	"gorm.io/gorm"
)

type LotRepository struct {
	db *gorm.DB
}

func NewLotRepository(db *gorm.DB) *LotRepository {
	return &LotRepository{db: db}
}
func (r *LotRepository) CreateLot(ctx context.Context, lot *models.LotPurchase) error {
	err := r.db.WithContext(ctx).Create(lot).Error
	if err != nil {
		return fmt.Errorf("create lot: %w", err)
	}
	return nil
}
func (r *LotRepository) GetAllLots(ctx context.Context) ([]models.LotPurchase, error) {
	var lots []models.LotPurchase
	err := r.db.WithContext(ctx).
		Preload("TrackedItems").
		Order("purchase_date DESC").
		Find(&lots).Error
	if err != nil {
		return nil, fmt.Errorf("get all lots: %w", err)
	}
	return lots, nil
}

func (r *LotRepository) GetLotByID(ctx context.Context, id uint) (*models.LotPurchase, error) {
	var lot models.LotPurchase
	err := r.db.WithContext(ctx).First(&lot, id).Error
	if err != nil {
		return nil, fmt.Errorf("get lot by id: %w", err)
	}
	return &lot, nil
}

func (r *LotRepository) GetLotWithItems(ctx context.Context, id uint) (*models.LotPurchase, error) {
	var lot models.LotPurchase
	err := r.db.WithContext(ctx).
		Preload("TrackedItems").
		Preload("TrackedItems.PokemonCard").
		First(&lot, id).Error
	if err != nil {
		return nil, fmt.Errorf("get lot with items: %w", err)
	}
	return &lot, nil
}

func (r *LotRepository) GetTotalCostNonRejected(ctx context.Context) (float64, error) {
	var total float64
	err := r.db.WithContext(ctx).
		Model(&models.LotPurchase{}).
		Where("status != ?", "REJECTED").
		Select("COALESCE(SUM(total_cost), 0)").
		Scan(&total).Error
	if err != nil {
		return 0, fmt.Errorf("get total cost: %w", err)
	}
	return total, nil
}
func (r *LotRepository) UpdateLot(ctx context.Context, lot *models.LotPurchase) error {
	err := r.db.WithContext(ctx).Save(lot).Error
	if err != nil {
		return fmt.Errorf("update lot: %w", err)
	}
	return nil
}

func (r *LotRepository) UpdateStatus(ctx context.Context, id uint, status string) error {
	err := r.db.WithContext(ctx).
		Model(&models.LotPurchase{}).
		Where("id = ?", id).
		Update("status", status).Error
	if err != nil {
		return fmt.Errorf("update lot status: %w", err)
	}
	return nil
}

func (r *LotRepository) Delete(ctx context.Context, id uint) error {
	err := r.db.WithContext(ctx).Delete(&models.LotPurchase{}, id).Error
	if err != nil {
		return fmt.Errorf("delete lot: %w", err)
	}
	return nil
}

type MonthlySpend struct {
	Month string
	Spend float64
	Count int64
}

func (r *LotRepository) MonthlySpend(ctx context.Context, months int) ([]MonthlySpend, error) {
	var rows []MonthlySpend
	err := r.db.WithContext(ctx).
		Model(&models.LotPurchase{}).
		Select("TO_CHAR(DATE_TRUNC('month', purchase_date), 'YYYY-MM') AS month, "+
			"COALESCE(SUM(total_cost), 0) AS spend, "+
			"COUNT(*) AS count").
		Where("status != ?", "REJECTED").
		Where("purchase_date >= NOW() - make_interval(months => ?)", months).
		Group("month").
		Order("month").
		Scan(&rows).Error
	if err != nil {
		return nil, fmt.Errorf("monthly spend: %w", err)
	}
	return rows, nil
}

type LotStatusCount struct {
	Status string
	Count  int64
}

func (r *LotRepository) CountByStatus(ctx context.Context) ([]LotStatusCount, error) {
	var rows []LotStatusCount
	err := r.db.WithContext(ctx).
		Model(&models.LotPurchase{}).
		Select("status, COUNT(*) AS count").
		Group("status").
		Scan(&rows).Error
	if err != nil {
		return nil, fmt.Errorf("count lots by status: %w", err)
	}
	return rows, nil
}

func (r *LotRepository) GetRecent(ctx context.Context, limit int) ([]models.LotPurchase, error) {
	var lots []models.LotPurchase
	err := r.db.WithContext(ctx).
		Order("purchase_date DESC").
		Limit(limit).
		Find(&lots).Error
	if err != nil {
		return nil, fmt.Errorf("get recent lots: %w", err)
	}
	return lots, nil
}
