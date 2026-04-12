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
		Where("tracked_items.purpose = ?", purpose).
		Joins("JOIN lot_purchases ON lot_purchases.id = tracked_items.lot_purchase_id").
		Where("lot_purchases.status = ?", "ACCEPTED").
		Find(&items).Error
	if err != nil {
		return nil, fmt.Errorf("get items by purpose: %w", err)
	}
	return items, nil
}

func (r *TrackedItemRepository) GetInventoryItems(ctx context.Context) ([]models.TrackedItem, error) {
	var items []models.TrackedItem
	err := r.db.WithContext(ctx).
		Preload("PokemonCard").
		Where("purpose = ?", "INVENTORY").
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
