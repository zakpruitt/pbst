package services

import (
	"context"
	"fmt"
	"time"

	"github.com/zakpruitt/pbst/internal/models"
	"github.com/zakpruitt/pbst/internal/repository"
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

func (s *GradingService) CreateWithItems(ctx context.Context, company, method string, itemIDs []uint) (*models.GradingSubmission, error) {
	count, err := s.gradingRepo.CountByCompany(ctx, company)
	if err != nil {
		return nil, fmt.Errorf("CreateWithItems: %w", err)
	}

	submission := &models.GradingSubmission{
		SubmissionName:   fmt.Sprintf("%s Submission #%d", company, count+1),
		Company:          company,
		SubmissionMethod: method,
		Status:           "PREPPING",
	}
	err = s.gradingRepo.CreateSubmission(ctx, submission)
	if err != nil {
		return nil, fmt.Errorf("CreateWithItems: %w", err)
	}

	if len(itemIDs) > 0 {
		err = s.itemRepo.AttachToSubmission(ctx, itemIDs, submission.ID)
		if err != nil {
			return nil, fmt.Errorf("CreateWithItems: attach: %w", err)
		}
		err = s.itemRepo.UpdateItemsStatusBySubmission(ctx, submission.ID, "IN_GRADING")
		if err != nil {
			return nil, fmt.Errorf("CreateWithItems: set IN_GRADING: %w", err)
		}
	}

	return submission, nil
}

func (s *GradingService) AdvanceStatus(ctx context.Context, id uint, newStatus string) error {
	err := s.gradingRepo.UpdateSubmissionStatus(ctx, id, newStatus)
	if err != nil {
		return fmt.Errorf("AdvanceStatus: %w", err)
	}
	return nil
}

func (s *GradingService) AttachItems(ctx context.Context, submissionID uint, itemIDs []uint) error {
	submission, err := s.gradingRepo.GetSubmissionByID(ctx, submissionID)
	if err != nil {
		return fmt.Errorf("AttachItems: %w", err)
	}
	if submission.Status != "PREPPING" {
		return fmt.Errorf("AttachItems: submission is not in PREPPING status")
	}
	err = s.itemRepo.AttachToSubmission(ctx, itemIDs, submissionID)
	if err != nil {
		return fmt.Errorf("AttachItems: %w", err)
	}
	if err = s.itemRepo.UpdateItemsStatusBySubmission(ctx, submissionID, "IN_GRADING"); err != nil {
		return fmt.Errorf("AttachItems: set IN_GRADING: %w", err)
	}
	return nil
}

func (s *GradingService) RecordReturn(ctx context.Context, submissionID uint, grades []ItemGrade) error {
	submission, err := s.gradingRepo.GetSubmissionByID(ctx, submissionID)
	if err != nil {
		return fmt.Errorf("RecordReturn: %w", err)
	}

	var totalUpcharge float64
	for _, g := range grades {
		details := models.GradedDetails{
			GradingCompany:  submission.Company,
			Grade:           g.Grade,
			GradingUpcharge: g.Upcharge,
		}
		err = s.itemRepo.UpdateGradedDetails(ctx, g.ItemID, details)
		if err != nil {
			return fmt.Errorf("RecordReturn: item %d: %w", g.ItemID, err)
		}
		totalUpcharge += g.Upcharge
	}

	err = s.gradingRepo.UpdateReturnDetails(ctx, submissionID, totalUpcharge, time.Now())
	if err != nil {
		return fmt.Errorf("RecordReturn: %w", err)
	}
	err = s.gradingRepo.UpdateSubmissionStatus(ctx, submissionID, "RETURNED")
	if err != nil {
		return fmt.Errorf("RecordReturn: %w", err)
	}
	return nil
}
