package services

import (
	"context"
	"database/sql"
	"fmt"

	"github.com/zakpruitt/pbst/internal/models"
	"github.com/zakpruitt/pbst/internal/repository"
)

type LotService interface {
	CreateLot(ctx context.Context, lot *models.LotPurchase) error
	GetLotByID(ctx context.Context, id uint) (*models.LotPurchase, error)
	GetLotWithItems(ctx context.Context, id uint) (*models.LotPurchase, error)
	GetAllLots(ctx context.Context) ([]models.LotPurchase, error)
	UpdateLot(ctx context.Context, lot *models.LotPurchase) error
	AcceptLot(ctx context.Context, id uint) error
	RejectLot(ctx context.Context, id uint) error
	DeleteLot(ctx context.Context, id uint) error
}

type lotService struct {
	lotRepo  repository.LotRepository
	itemRepo repository.TrackedItemRepository
}

func NewLotService(lotRepo repository.LotRepository, itemRepo repository.TrackedItemRepository) LotService {
	return &lotService{lotRepo: lotRepo, itemRepo: itemRepo}
}

func (s *lotService) CreateLot(ctx context.Context, lot *models.LotPurchase) error {
	return s.lotRepo.CreateLot(ctx, lot)
}

func (s *lotService) GetLotByID(ctx context.Context, id uint) (*models.LotPurchase, error) {
	return s.lotRepo.GetLotByID(ctx, id)
}

func (s *lotService) GetLotWithItems(ctx context.Context, id uint) (*models.LotPurchase, error) {
	return s.lotRepo.GetLotWithItems(ctx, id)
}

func (s *lotService) GetAllLots(ctx context.Context) ([]models.LotPurchase, error) {
	return s.lotRepo.GetAllLots(ctx)
}

func (s *lotService) UpdateLot(ctx context.Context, lot *models.LotPurchase) error {
	return s.lotRepo.UpdateLot(ctx, lot)
}

func (s *lotService) AcceptLot(ctx context.Context, id uint) error {
	lot, err := s.lotRepo.GetLotByID(ctx, id)
	if err != nil {
		return fmt.Errorf("load lot: %w", err)
	}

	snapshot, err := lot.ParseSnapshot()
	if err != nil {
		return fmt.Errorf("parse snapshot: %w", err)
	}

	for _, item := range snapshot {
		if !item.IsTracked {
			continue
		}
		if err := s.itemRepo.AddTrackedItem(ctx, snapshotToTrackedItem(lot, item)); err != nil {
			return fmt.Errorf("create tracked item: %w", err)
		}
	}

	return s.lotRepo.UpdateStatus(ctx, id, "ACCEPTED")
}

func (s *lotService) RejectLot(ctx context.Context, id uint) error {
	return s.lotRepo.UpdateStatus(ctx, id, "REJECTED")
}

// DeleteLot removes a lot and all of its tracked items. Use with care — this
// wipes the inventory that originated from the lot, not just the lot record.
func (s *lotService) DeleteLot(ctx context.Context, id uint) error {
	if err := s.itemRepo.DeleteByLotID(ctx, id); err != nil {
		return fmt.Errorf("delete tracked items: %w", err)
	}
	if err := s.lotRepo.Delete(ctx, id); err != nil {
		return fmt.Errorf("delete lot: %w", err)
	}
	return nil
}

func snapshotToTrackedItem(lot *models.LotPurchase, item models.SnapshotItem) *models.TrackedItem {
	qty := item.Qty
	if qty <= 0 {
		qty = 1
	}

	purpose := item.Purpose
	if purpose == "" && item.IsTracked {
		purpose = "INVENTORY"
	}

	itemType := item.ItemType
	if itemType == "" {
		itemType = "RAW_CARD"
	}

	ti := &models.TrackedItem{
		LotPurchaseID:         sql.NullInt64{Int64: int64(lot.ID), Valid: true},
		AcquisitionDate:       lot.PurchaseDate,
		CostBasis:             item.Offered / float64(qty),
		MarketValueAtPurchase: item.MarketPrice,
		Purpose:               purpose,
		ItemType:              itemType,
	}

	if item.PokemonCardID != "" {
		ti.PokemonCardID = sql.NullString{String: item.PokemonCardID, Valid: true}
	} else {
		ti.ManualNameOverride = sql.NullString{String: item.Name, Valid: true}
	}

	if item.ItemType == "GRADED_CARD" && item.GradingCompany != "" {
		ti.GradedDetails = &models.GradedDetails{
			GradingCompany: item.GradingCompany,
			Grade:          item.Grade,
		}
	}

	return ti
}
