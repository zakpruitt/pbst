package services

import (
	"context"
	"database/sql"
	"encoding/json"
	"fmt"

	"github.com/zakpruitt/pbst/internal/models"
	"github.com/zakpruitt/pbst/internal/repository"
)

type LotService struct {
	lotRepo  *repository.LotRepository
	itemRepo *repository.TrackedItemRepository
}

func NewLotService(lotRepo *repository.LotRepository, itemRepo *repository.TrackedItemRepository) *LotService {
	return &LotService{lotRepo: lotRepo, itemRepo: itemRepo}
}

func (s *LotService) AcceptLot(ctx context.Context, id uint) error {
	lot, err := s.lotRepo.GetLotByID(ctx, id)
	if err != nil {
		return fmt.Errorf("AcceptLot load: %w", err)
	}

	var snapshot []models.SnapshotItem
	if lot.LotContentSnapshot != "" {
		if err := json.Unmarshal([]byte(lot.LotContentSnapshot), &snapshot); err != nil {
			return fmt.Errorf("AcceptLot parse snapshot: %w", err)
		}
	}

	for _, item := range snapshot {
		if !item.IsTracked {
			continue
		}
		ti := snapshotToTrackedItem(lot, item)
		if err := s.itemRepo.AddTrackedItem(ctx, ti); err != nil {
			return fmt.Errorf("AcceptLot create item: %w", err)
		}
	}

	return s.lotRepo.UpdateStatus(ctx, id, "ACCEPTED")
}

// snapshotToTrackedItem builds a TrackedItem from a lot and one of its snapshot entries.
func snapshotToTrackedItem(lot *models.LotPurchase, item models.SnapshotItem) *models.TrackedItem {
	qty := item.Qty
	if qty <= 0 {
		qty = 1
	}

	purpose := item.Purpose
	if purpose == "" && item.IsTracked {
		purpose = "INVENTORY"
	}

	ti := &models.TrackedItem{
		LotPurchaseID:         lot.ID,
		AcquisitionDate:       lot.PurchaseDate,
		CostBasis:             item.Offered / float64(qty),
		MarketValueAtPurchase: item.MarketPrice,
		Purpose:               purpose,
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

func (s *LotService) RejectLot(ctx context.Context, id uint) error {
	return s.lotRepo.UpdateStatus(ctx, id, "REJECTED")
}
