package repository

import (
	"context"
	"fmt"
	"time"

	"github.com/zakpruitt/pbst/internal/models"
	"gorm.io/gorm"
)

type GradingRepository struct {
	db *gorm.DB
}

func NewGradingRepository(db *gorm.DB) *GradingRepository {
	return &GradingRepository{db: db}
}
func (r *GradingRepository) CreateSubmission(ctx context.Context, submission *models.GradingSubmission) error {
	err := r.db.WithContext(ctx).Create(submission).Error
	if err != nil {
		return fmt.Errorf("create submission: %w", err)
	}
	return nil
}
func (r *GradingRepository) GetAllSubmissions(ctx context.Context) ([]models.GradingSubmission, error) {
	var submissions []models.GradingSubmission
	err := r.db.WithContext(ctx).
		Preload("Items").
		Order("created_at DESC").
		Find(&submissions).Error
	if err != nil {
		return nil, fmt.Errorf("get all submissions: %w", err)
	}
	return submissions, nil
}

func (r *GradingRepository) GetSubmissionByID(ctx context.Context, id uint) (*models.GradingSubmission, error) {
	var submission models.GradingSubmission
	err := r.db.WithContext(ctx).
		Preload("Items").
		Preload("Items.PokemonCard").
		Preload("Items.LotPurchase").
		First(&submission, id).Error
	if err != nil {
		return nil, fmt.Errorf("get submission by id: %w", err)
	}
	return &submission, nil
}

func (r *GradingRepository) CountByCompany(ctx context.Context, company string) (int64, error) {
	var count int64
	err := r.db.WithContext(ctx).
		Model(&models.GradingSubmission{}).
		Where("company = ?", company).
		Count(&count).Error
	if err != nil {
		return 0, fmt.Errorf("count by company: %w", err)
	}
	return count, nil
}
func (r *GradingRepository) UpdateSubmission(ctx context.Context, submission *models.GradingSubmission) error {
	err := r.db.WithContext(ctx).
		Model(submission).
		Select("company", "submission_method", "cost_per_card", "tax_rate", "submission_cost", "notes").
		Updates(submission).Error
	if err != nil {
		return fmt.Errorf("update submission: %w", err)
	}
	return nil
}

func (r *GradingRepository) UpdateSubmissionStatus(ctx context.Context, id uint, status string) error {
	err := r.db.WithContext(ctx).
		Model(&models.GradingSubmission{}).
		Where("id = ?", id).
		Update("status", status).Error
	if err != nil {
		return fmt.Errorf("update submission status: %w", err)
	}
	return nil
}

func (r *GradingRepository) SetSendDate(ctx context.Context, id uint, date time.Time) error {
	err := r.db.WithContext(ctx).
		Model(&models.GradingSubmission{}).
		Where("id = ?", id).
		Update("send_date", date).Error
	if err != nil {
		return fmt.Errorf("set send date: %w", err)
	}
	return nil
}

func (r *GradingRepository) DeleteSubmission(ctx context.Context, id uint) error {
	err := r.db.WithContext(ctx).Delete(&models.GradingSubmission{}, id).Error
	if err != nil {
		return fmt.Errorf("delete submission: %w", err)
	}
	return nil
}

func (r *GradingRepository) UpdateReturnDetails(ctx context.Context, id uint, upchargeTotal float64, returnDate time.Time) error {
	err := r.db.WithContext(ctx).
		Model(&models.GradingSubmission{}).
		Where("id = ?", id).
		Updates(map[string]any{
			"upcharge_total": upchargeTotal,
			"return_date":    returnDate,
		}).Error
	if err != nil {
		return fmt.Errorf("update return details: %w", err)
	}
	return nil
}
