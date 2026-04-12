package services

import (
	"context"
	"database/sql"
	"fmt"
	"time"

	"github.com/zakpruitt/pbst/internal/models"
	"github.com/zakpruitt/pbst/internal/repository"
)

const (
	DefaultCostPerCard = 20.0
	MissouriTaxRate    = 0.04225
)

type GradingService struct {
	gradingRepo *repository.GradingRepository
	itemRepo    *repository.TrackedItemRepository
}

func NewGradingService(gradingRepo *repository.GradingRepository, itemRepo *repository.TrackedItemRepository) *GradingService {
	return &GradingService{gradingRepo: gradingRepo, itemRepo: itemRepo}
}

type ItemGrade struct {
	ItemID   uint
	Grade    string
	Upcharge float64
}

func CalculateSubmissionCost(numItems int, costPerCard, taxRate float64) float64 {
	return float64(numItems) * costPerCard * (1 + taxRate)
}

func normalizeCostPerCard(cost float64) float64 {
	if cost <= 0 {
		return DefaultCostPerCard
	}
	return cost
}

func (s *GradingService) CreateWithItems(ctx context.Context, company, method string, costPerCard float64, notes sql.NullString, itemIDs []uint) (*models.GradingSubmission, error) {
	costPerCard = normalizeCostPerCard(costPerCard)

	count, err := s.gradingRepo.CountByCompany(ctx, company)
	if err != nil {
		return nil, fmt.Errorf("count submissions: %w", err)
	}

	submission := &models.GradingSubmission{
		SubmissionName:   fmt.Sprintf("%s Submission #%d", company, count+1),
		Company:          company,
		SubmissionMethod: method,
		Status:           "PREPPING",
		CostPerCard:      costPerCard,
		TaxRate:          MissouriTaxRate,
		SubmissionCost:   CalculateSubmissionCost(len(itemIDs), costPerCard, MissouriTaxRate),
		Notes:            notes,
	}
	if err = s.gradingRepo.CreateSubmission(ctx, submission); err != nil {
		return nil, fmt.Errorf("create submission: %w", err)
	}

	if len(itemIDs) > 0 {
		if err = s.itemRepo.AttachToSubmission(ctx, itemIDs, submission.ID); err != nil {
			return nil, fmt.Errorf("attach items: %w", err)
		}
		if err = s.itemRepo.UpdateItemsStatusBySubmission(ctx, submission.ID, "IN_GRADING"); err != nil {
			return nil, fmt.Errorf("set items in grading: %w", err)
		}
	}

	return submission, nil
}

func (s *GradingService) UpdateSubmission(ctx context.Context, id uint, company, method string, costPerCard float64, notes sql.NullString, itemIDs []uint) error {
	submission, err := s.gradingRepo.GetSubmissionByID(ctx, id)
	if err != nil {
		return fmt.Errorf("load submission: %w", err)
	}

	if err = s.itemRepo.DetachFromSubmission(ctx, id); err != nil {
		return fmt.Errorf("detach items: %w", err)
	}

	if len(itemIDs) > 0 {
		if err = s.itemRepo.AttachToSubmission(ctx, itemIDs, id); err != nil {
			return fmt.Errorf("attach items: %w", err)
		}
		if err = s.itemRepo.UpdateItemsStatusBySubmission(ctx, id, "IN_GRADING"); err != nil {
			return fmt.Errorf("set items in grading: %w", err)
		}
	}

	submission.Company = company
	submission.SubmissionMethod = method
	submission.CostPerCard = normalizeCostPerCard(costPerCard)
	submission.TaxRate = MissouriTaxRate
	submission.SubmissionCost = CalculateSubmissionCost(len(itemIDs), submission.CostPerCard, MissouriTaxRate)
	submission.Notes = notes

	if err = s.gradingRepo.UpdateSubmission(ctx, submission); err != nil {
		return fmt.Errorf("save submission: %w", err)
	}
	return nil
}

func (s *GradingService) AdvanceStatus(ctx context.Context, id uint, newStatus string) error {
	if err := s.gradingRepo.UpdateSubmissionStatus(ctx, id, newStatus); err != nil {
		return fmt.Errorf("update status: %w", err)
	}
	if newStatus == "IN_TRANSIT" {
		if err := s.gradingRepo.SetSendDate(ctx, id, time.Now()); err != nil {
			return fmt.Errorf("set send date: %w", err)
		}
	}
	return nil
}

func (s *GradingService) RecordReturn(ctx context.Context, submissionID uint, grades []ItemGrade) error {
	submission, err := s.gradingRepo.GetSubmissionByID(ctx, submissionID)
	if err != nil {
		return fmt.Errorf("load submission: %w", err)
	}

	var totalUpcharge float64
	for _, g := range grades {
		details := models.GradedDetails{
			GradingCompany:  submission.Company,
			Grade:           g.Grade,
			GradingUpcharge: g.Upcharge,
		}
		if err = s.itemRepo.UpdateGradedDetails(ctx, g.ItemID, details); err != nil {
			return fmt.Errorf("update item %d grade: %w", g.ItemID, err)
		}
		totalUpcharge += g.Upcharge
	}

	if err = s.gradingRepo.UpdateReturnDetails(ctx, submissionID, totalUpcharge, time.Now()); err != nil {
		return fmt.Errorf("update return details: %w", err)
	}
	if err = s.gradingRepo.UpdateSubmissionStatus(ctx, submissionID, "RETURNED"); err != nil {
		return fmt.Errorf("set returned status: %w", err)
	}
	return nil
}
