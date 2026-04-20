package repository

import (
	"context"
	"fmt"

	"github.com/zakpruitt/pbst/internal/models"
	"gorm.io/gorm"
)

type TrackedItemRepository interface {
	AddTrackedItem(ctx context.Context, item *models.TrackedItem) error
	GetByID(ctx context.Context, id uint) (*models.TrackedItem, error)
	GetItemsByPurpose(ctx context.Context, purpose string) ([]models.TrackedItem, error)
	GetAvailableInventory(ctx context.Context) ([]models.TrackedItem, error)
	CountByPurpose(ctx context.Context, purpose string) (int64, error)
	CountByItemType(ctx context.Context) ([]ItemTypeCount, error)
	InventoryTotals(ctx context.Context) (cost, market float64, err error)
	Update(ctx context.Context, item *models.TrackedItem) error
	UpdateGradedDetails(ctx context.Context, itemID uint, details models.GradedDetails) error
	UpdateItemsStatusBySubmission(ctx context.Context, submissionID uint, newPurpose string) error
	AttachToSubmission(ctx context.Context, itemIDs []uint, submissionID uint) error
	DetachFromSubmission(ctx context.Context, submissionID uint) error
	AttachToSale(ctx context.Context, itemIDs []uint, saleID uint) error
	DetachFromSale(ctx context.Context, saleID uint) error
	Delete(ctx context.Context, id uint) error
	DeleteByLotID(ctx context.Context, lotID uint) error
}

type trackedItemRepository struct {
	db *gorm.DB
}

func NewTrackedItemRepository(db *gorm.DB) TrackedItemRepository {
	return &trackedItemRepository{db: db}
}

type ItemTypeCount struct {
	ItemType string
	Count    int64
	Market   float64
	Cost     float64
}

func (r *trackedItemRepository) AddTrackedItem(ctx context.Context, item *models.TrackedItem) error {
	err := r.db.WithContext(ctx).Create(item).Error
	if err != nil {
		return fmt.Errorf("add tracked item: %w", err)
	}
	return nil
}

func (r *trackedItemRepository) GetByID(ctx context.Context, id uint) (*models.TrackedItem, error) {
	var item models.TrackedItem
	err := r.db.WithContext(ctx).
		Preload("PokemonCard").
		Preload("SealedProduct").
		Preload("LotPurchase").
		Preload("GradingSubmission").
		First(&item, id).Error
	if err != nil {
		return nil, fmt.Errorf("get tracked item: %w", err)
	}
	return &item, nil
}

func (r *trackedItemRepository) GetItemsByPurpose(ctx context.Context, purpose string) ([]models.TrackedItem, error) {
	var items []models.TrackedItem
	err := r.db.WithContext(ctx).
		Preload("PokemonCard").
		Preload("SealedProduct").
		Preload("LotPurchase").
		Preload("GradingSubmission").
		Where("tracked_items.purpose = ?", purpose).
		Where("tracked_items.lot_purchase_id IS NULL OR tracked_items.lot_purchase_id IN (?)",
			r.db.Table("lot_purchases").Select("id").Where("status = ?", "ACCEPTED")).
		Find(&items).Error
	if err != nil {
		return nil, fmt.Errorf("get items by purpose: %w", err)
	}
	return items, nil
}

func (r *trackedItemRepository) GetAvailableInventory(ctx context.Context) ([]models.TrackedItem, error) {
	var items []models.TrackedItem
	err := r.db.WithContext(ctx).
		Preload("PokemonCard").
		Where("purpose = ?", "INVENTORY").
		Where("sale_id IS NULL").
		Find(&items).Error
	if err != nil {
		return nil, fmt.Errorf("get available inventory items: %w", err)
	}
	return items, nil
}

func (r *trackedItemRepository) CountByPurpose(ctx context.Context, purpose string) (int64, error) {
	var count int64
	err := r.db.WithContext(ctx).
		Model(&models.TrackedItem{}).
		Where("purpose = ?", purpose).
		Where("lot_purchase_id IS NULL OR lot_purchase_id IN (?)",
			r.db.Table("lot_purchases").Select("id").Where("status = ?", "ACCEPTED")).
		Count(&count).Error
	if err != nil {
		return 0, fmt.Errorf("count by purpose: %w", err)
	}
	return count, nil
}

// CountByItemType groups live inventory (purpose=INVENTORY, unsold) by item_type,
// returning counts plus market/cost totals for the dashboard breakdown.
func (r *trackedItemRepository) CountByItemType(ctx context.Context) ([]ItemTypeCount, error) {
	var rows []ItemTypeCount
	err := r.db.WithContext(ctx).
		Model(&models.TrackedItem{}).
		Select("item_type, COUNT(*) AS count, "+
			"COALESCE(SUM(market_value_at_purchase), 0) AS market, "+
			"COALESCE(SUM(cost_basis), 0) AS cost").
		Where("purpose = ?", "INVENTORY").
		Where("sale_id IS NULL").
		Group("item_type").
		Scan(&rows).Error
	if err != nil {
		return nil, fmt.Errorf("count by item type: %w", err)
	}
	return rows, nil
}

func (r *trackedItemRepository) InventoryTotals(ctx context.Context) (cost, market float64, err error) {
	var row struct {
		Cost   float64
		Market float64
	}
	err = r.db.WithContext(ctx).
		Model(&models.TrackedItem{}).
		Select("COALESCE(SUM(cost_basis), 0) AS cost, "+
			"COALESCE(SUM(market_value_at_purchase), 0) AS market").
		Where("purpose = ?", "INVENTORY").
		Where("sale_id IS NULL").
		Scan(&row).Error
	if err != nil {
		return 0, 0, fmt.Errorf("inventory totals: %w", err)
	}
	return row.Cost, row.Market, nil
}

func (r *trackedItemRepository) Update(ctx context.Context, item *models.TrackedItem) error {
	err := r.db.WithContext(ctx).Save(item).Error
	if err != nil {
		return fmt.Errorf("update tracked item: %w", err)
	}
	return nil
}

func (r *trackedItemRepository) UpdateGradedDetails(ctx context.Context, itemID uint, details models.GradedDetails) error {
	err := r.db.WithContext(ctx).
		Model(&models.TrackedItem{}).
		Where("id = ?", itemID).
		Updates(map[string]any{
			"grading_company":  details.GradingCompany,
			"grade":            details.Grade,
			"grading_upcharge": details.GradingUpcharge,
			"purpose":          "INVENTORY",
			"item_type":        "GRADED_CARD",
		}).Error
	if err != nil {
		return fmt.Errorf("update graded details: %w", err)
	}
	return nil
}

func (r *trackedItemRepository) UpdateItemsStatusBySubmission(ctx context.Context, submissionID uint, newPurpose string) error {
	err := r.db.WithContext(ctx).
		Model(&models.TrackedItem{}).
		Where("grading_submission_id = ?", submissionID).
		Update("purpose", newPurpose).Error
	if err != nil {
		return fmt.Errorf("update items status: %w", err)
	}
	return nil
}

func (r *trackedItemRepository) AttachToSubmission(ctx context.Context, itemIDs []uint, submissionID uint) error {
	err := r.db.WithContext(ctx).
		Model(&models.TrackedItem{}).
		Where("id IN ?", itemIDs).
		Update("grading_submission_id", submissionID).Error
	if err != nil {
		return fmt.Errorf("attach to submission: %w", err)
	}
	return nil
}

func (r *trackedItemRepository) DetachFromSubmission(ctx context.Context, submissionID uint) error {
	err := r.db.WithContext(ctx).
		Model(&models.TrackedItem{}).
		Where("grading_submission_id = ?", submissionID).
		Updates(map[string]any{
			"grading_submission_id": nil,
			"purpose":               "INVENTORY",
		}).Error
	if err != nil {
		return fmt.Errorf("detach from submission: %w", err)
	}
	return nil
}

func (r *trackedItemRepository) AttachToSale(ctx context.Context, itemIDs []uint, saleID uint) error {
	if len(itemIDs) == 0 {
		return nil
	}
	err := r.db.WithContext(ctx).
		Model(&models.TrackedItem{}).
		Where("id IN ?", itemIDs).
		Updates(map[string]any{
			"sale_id": saleID,
			"purpose": "SOLD",
		}).Error
	if err != nil {
		return fmt.Errorf("attach to sale: %w", err)
	}
	return nil
}

func (r *trackedItemRepository) DetachFromSale(ctx context.Context, saleID uint) error {
	err := r.db.WithContext(ctx).
		Model(&models.TrackedItem{}).
		Where("sale_id = ?", saleID).
		Updates(map[string]any{
			"sale_id": nil,
			"purpose": "INVENTORY",
		}).Error
	if err != nil {
		return fmt.Errorf("detach from sale: %w", err)
	}
	return nil
}

func (r *trackedItemRepository) Delete(ctx context.Context, id uint) error {
	err := r.db.WithContext(ctx).Delete(&models.TrackedItem{}, id).Error
	if err != nil {
		return fmt.Errorf("delete tracked item: %w", err)
	}
	return nil
}

func (r *trackedItemRepository) DeleteByLotID(ctx context.Context, lotID uint) error {
	err := r.db.WithContext(ctx).
		Where("lot_purchase_id = ?", lotID).
		Delete(&models.TrackedItem{}).Error
	if err != nil {
		return fmt.Errorf("delete items by lot: %w", err)
	}
	return nil
}
