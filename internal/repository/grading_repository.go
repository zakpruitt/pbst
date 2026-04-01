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
	err := r.db.WithContext(ctx).
		Create(submission).Error
	if err != nil {
		return fmt.Errorf("CreateSubmission: %w", err)
	}

	return nil
}

func (r *GradingRepository) CountByCompany(ctx context.Context, company string) (int64, error) {
	var count int64

	err := r.db.WithContext(ctx).
		Model(&models.GradingSubmission{}).
		Where("company = ?", company).
		Count(&count).Error
	if err != nil {
		return 0, fmt.Errorf("CountByCompany: %w", err)
	}

	return count, nil
}

func (r *GradingRepository) GetAllSubmissions(ctx context.Context) ([]models.GradingSubmission, error) {
	var submissions []models.GradingSubmission

	err := r.db.WithContext(ctx).
		Preload("Items").
		Find(&submissions).Error
	if err != nil {
		return nil, fmt.Errorf("GetAllSubmissions: %w", err)
	}

	return submissions, nil
}

func (r *GradingRepository) GetSubmissionByID(ctx context.Context, id uint) (*models.GradingSubmission, error) {
	var submission models.GradingSubmission

	err := r.db.WithContext(ctx).
		Preload("Items").
		Preload("Items.PokemonCard").
		First(&submission, id).Error
	if err != nil {
		return nil, fmt.Errorf("GetSubmissionByID: %w", err)
	}

	return &submission, nil
}

func (r *GradingRepository) UpdateSubmissionStatus(ctx context.Context, id uint, status string) error {
	err := r.db.WithContext(ctx).
		Model(&models.GradingSubmission{}).
		Where("id = ?", id).
		Update("status", status).Error
	if err != nil {
		return fmt.Errorf("UpdateSubmissionStatus: %w", err)
	}

	return nil
}

func (r *GradingRepository) UpdateReturnDetails(ctx context.Context, id uint, totalCost float64, returnDate time.Time) error {
	err := r.db.WithContext(ctx).
		Model(&models.GradingSubmission{}).
		Where("id = ?", id).
		Updates(map[string]any{
			"total_grading_cost": totalCost,
			"return_date":        returnDate,
		}).Error
	if err != nil {
		return fmt.Errorf("UpdateReturnDetails: %w", err)
	}

	return nil
}
