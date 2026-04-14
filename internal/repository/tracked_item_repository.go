package repository

import (
	"context"
	"fmt"

	"github.com/zakpruitt/pbst/internal/models"
	"gorm.io/gorm"
)

type TrackedItemRepository struct {
	db *gorm.DB
}

func NewTrackedItemRepository(db *gorm.DB) *TrackedItemRepository {
	return &TrackedItemRepository{db: db}
}

func (r *TrackedItemRepository) AddTrackedItem(ctx context.Context, item *models.TrackedItem) error {
	err := r.db.WithContext(ctx).Create(item).Error
	if err != nil {
		return fmt.Errorf("add tracked item: %w", err)
	}
	return nil
}

func (r *TrackedItemRepository) GetItemsByPurpose(ctx context.Context, purpose string) ([]models.TrackedItem, error) {
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

func (r *TrackedItemRepository) GetByID(ctx context.Context, id uint) (*models.TrackedItem, error) {
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

func (r *TrackedItemRepository) Update(ctx context.Context, item *models.TrackedItem) error {
	err := r.db.WithContext(ctx).Save(item).Error
	if err != nil {
		return fmt.Errorf("update tracked item: %w", err)
	}
	return nil
}

func (r *TrackedItemRepository) Delete(ctx context.Context, id uint) error {
	err := r.db.WithContext(ctx).Delete(&models.TrackedItem{}, id).Error
	if err != nil {
		return fmt.Errorf("delete tracked item: %w", err)
	}
	return nil
}

func (r *TrackedItemRepository) GetInventoryItems(ctx context.Context) ([]models.TrackedItem, error) {
	var items []models.TrackedItem
	err := r.db.WithContext(ctx).
		Preload("PokemonCard").
		Where("purpose = ?", "INVENTORY").
		Where("sale_id IS NULL").
		Find(&items).Error
	if err != nil {
		return nil, fmt.Errorf("get inventory items: %w", err)
	}
	return items, nil
}

func (r *TrackedItemRepository) CountByPurpose(ctx context.Context, purpose string) (int64, error) {
	var count int64
	err := r.db.WithContext(ctx).
		Model(&models.TrackedItem{}).
		Where("tracked_items.purpose = ?", purpose).
		Joins("JOIN lot_purchases ON lot_purchases.id = tracked_items.lot_purchase_id").
		Where("lot_purchases.status = ?", "ACCEPTED").
		Count(&count).Error
	if err != nil {
		return 0, fmt.Errorf("count by purpose: %w", err)
	}
	return count, nil
}

func (r *TrackedItemRepository) AttachToSubmission(ctx context.Context, itemIDs []uint, submissionID uint) error {
	err := r.db.WithContext(ctx).
		Model(&models.TrackedItem{}).
		Where("id IN ?", itemIDs).
		Update("grading_submission_id", submissionID).Error
	if err != nil {
		return fmt.Errorf("attach to submission: %w", err)
	}
	return nil
}

func (r *TrackedItemRepository) DetachFromSubmission(ctx context.Context, submissionID uint) error {
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

func (r *TrackedItemRepository) UpdateItemsStatusBySubmission(ctx context.Context, submissionID uint, newPurpose string) error {
	err := r.db.WithContext(ctx).
		Model(&models.TrackedItem{}).
		Where("grading_submission_id = ?", submissionID).
		Update("purpose", newPurpose).Error
	if err != nil {
		return fmt.Errorf("update items status: %w", err)
	}
	return nil
}

func (r *TrackedItemRepository) AttachToSale(ctx context.Context, itemIDs []uint, saleID uint) error {
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

func (r *TrackedItemRepository) DetachFromSale(ctx context.Context, saleID uint) error {
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

func (r *TrackedItemRepository) DeleteByLotID(ctx context.Context, lotID uint) error {
	err := r.db.WithContext(ctx).
		Where("lot_purchase_id = ?", lotID).
		Delete(&models.TrackedItem{}).Error
	if err != nil {
		return fmt.Errorf("delete items by lot: %w", err)
	}
	return nil
}

func (r *TrackedItemRepository) UpdateGradedDetails(ctx context.Context, itemID uint, details models.GradedDetails) error {
	err := r.db.WithContext(ctx).
		Model(&models.TrackedItem{}).
		Where("id = ?", itemID).
		Updates(map[string]any{
			"grading_company":  details.GradingCompany,
			"grade":            details.Grade,
			"grading_upcharge": details.GradingUpcharge,
			"purpose":          "INVENTORY",
		}).Error
	if err != nil {
		return fmt.Errorf("update graded details: %w", err)
	}
	return nil
}
