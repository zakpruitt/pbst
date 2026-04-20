package services

import (
	"context"
	"database/sql"
	"fmt"
	"time"

	"github.com/zakpruitt/pbst/internal/models"
	"github.com/zakpruitt/pbst/internal/repository"
)

type GradingService interface {
	CreateWithItems(ctx context.Context, company, method string, submissionCost float64, notes sql.NullString, itemIDs []uint) (*models.GradingSubmission, error)
	GetAllSubmissions(ctx context.Context) ([]models.GradingSubmission, error)
	GetSubmissionByID(ctx context.Context, id uint) (*models.GradingSubmission, error)
	UpdateSubmission(ctx context.Context, id uint, company, method string, submissionCost float64, notes sql.NullString, itemIDs []uint) error
	DeleteSubmission(ctx context.Context, id uint) error
	AdvanceStatus(ctx context.Context, id uint, newStatus string) error
	RecordReturn(ctx context.Context, submissionID uint, grades []ItemGrade) error
}

type gradingService struct {
	gradingRepo repository.GradingRepository
	itemRepo    repository.TrackedItemRepository
}

func NewGradingService(gradingRepo repository.GradingRepository, itemRepo repository.TrackedItemRepository) GradingService {
	return &gradingService{gradingRepo: gradingRepo, itemRepo: itemRepo}
}

type ItemGrade struct {
	ItemID   uint
	Grade    string
	Upcharge float64
}

func (s *gradingService) CreateWithItems(ctx context.Context, company, method string, submissionCost float64, notes sql.NullString, itemIDs []uint) (*models.GradingSubmission, error) {
	count, err := s.gradingRepo.CountByCompany(ctx, company)
	if err != nil {
		return nil, fmt.Errorf("count submissions: %w", err)
	}

	submission := &models.GradingSubmission{
		SubmissionName:   fmt.Sprintf("%s Submission #%d", company, count+1),
		Company:          company,
		SubmissionMethod: method,
		Status:           "PREPPING",
		CostPerCard:      derivedCostPerCard(submissionCost, len(itemIDs)),
		TaxRate:          0,
		SubmissionCost:   submissionCost,
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

func (s *gradingService) GetAllSubmissions(ctx context.Context) ([]models.GradingSubmission, error) {
	return s.gradingRepo.GetAllSubmissions(ctx)
}

func (s *gradingService) GetSubmissionByID(ctx context.Context, id uint) (*models.GradingSubmission, error) {
	return s.gradingRepo.GetSubmissionByID(ctx, id)
}

func (s *gradingService) UpdateSubmission(ctx context.Context, id uint, company, method string, submissionCost float64, notes sql.NullString, itemIDs []uint) error {
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
	submission.SubmissionCost = submissionCost
	submission.CostPerCard = derivedCostPerCard(submissionCost, len(itemIDs))
	submission.TaxRate = 0
	submission.Notes = notes

	if err = s.gradingRepo.UpdateSubmission(ctx, submission); err != nil {
		return fmt.Errorf("save submission: %w", err)
	}
	return nil
}

func (s *gradingService) DeleteSubmission(ctx context.Context, id uint) error {
	if err := s.itemRepo.DetachFromSubmission(ctx, id); err != nil {
		return fmt.Errorf("detach items: %w", err)
	}
	if err := s.gradingRepo.DeleteSubmission(ctx, id); err != nil {
		return fmt.Errorf("delete submission: %w", err)
	}
	return nil
}

func (s *gradingService) AdvanceStatus(ctx context.Context, id uint, newStatus string) error {
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

func (s *gradingService) RecordReturn(ctx context.Context, submissionID uint, grades []ItemGrade) error {
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

func derivedCostPerCard(submissionCost float64, numItems int) float64 {
	if numItems <= 0 {
		return 0
	}
	return submissionCost / float64(numItems)
}
