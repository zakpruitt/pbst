package services

import (
	"context"
	"database/sql"
	"encoding/json"
	"fmt"
	"time"

	"github.com/zakpruitt/pbst/internal/models"
	"github.com/zakpruitt/pbst/internal/repository"
)

type InventoryService interface {
	CreateItems(ctx context.Context, input CreateItemsInput) error
	GetItemByID(ctx context.Context, id uint) (*models.TrackedItem, error)
	GetItemsByPurpose(ctx context.Context, purpose string) ([]models.TrackedItem, error)
	GetInventoryForSaleConfirm(ctx context.Context, sale *models.Sale) ([]models.TrackedItem, error)
	UpdateItem(ctx context.Context, item *models.TrackedItem) error
	DeleteItem(ctx context.Context, id uint) error
}

type inventoryService struct {
	itemRepo repository.TrackedItemRepository
}

func NewInventoryService(itemRepo repository.TrackedItemRepository) InventoryService {
	return &inventoryService{itemRepo: itemRepo}
}

type CreateItemsInput struct {
	SnapshotJSON    string
	Purpose         string
	AcquisitionDate time.Time
}

type inventoryRowInput struct {
	Name            string  `json:"name"`
	ItemType        string  `json:"item_type"`
	CostBasis       float64 `json:"cost_basis"`
	MarketValue     float64 `json:"market_value"`
	PokemonCardID   string  `json:"pokemon_card_id"`
	SealedProductID string  `json:"sealed_product_id"`
	GradingCompany  string  `json:"grading_company"`
	Grade           string  `json:"grade"`
}

func (s *inventoryService) CreateItems(ctx context.Context, input CreateItemsInput) error {
	if input.SnapshotJSON == "" {
		return nil
	}

	var rows []inventoryRowInput
	if err := json.Unmarshal([]byte(input.SnapshotJSON), &rows); err != nil {
		return fmt.Errorf("parse snapshot: %w", err)
	}

	purpose := input.Purpose
	if purpose == "" {
		purpose = "INVENTORY"
	}

	for _, row := range rows {
		if row.ItemType == "" {
			row.ItemType = "OTHER"
		}
		item := &models.TrackedItem{
			CostBasis:             row.CostBasis,
			MarketValueAtPurchase: row.MarketValue,
			AcquisitionDate:       input.AcquisitionDate,
			Purpose:               purpose,
			ItemType:              row.ItemType,
			PokemonCardID:         nullStr(row.PokemonCardID),
			SealedProductID:       nullStr(row.SealedProductID),
		}
		if row.PokemonCardID == "" && row.SealedProductID == "" {
			item.ManualNameOverride = nullStr(row.Name)
		}
		if row.ItemType == "GRADED_CARD" {
			item.GradedDetails = &models.GradedDetails{
				GradingCompany: row.GradingCompany,
				Grade:          row.Grade,
			}
		}
		if err := s.itemRepo.AddTrackedItem(ctx, item); err != nil {
			return fmt.Errorf("add item: %w", err)
		}
	}
	return nil
}

func (s *inventoryService) GetItemByID(ctx context.Context, id uint) (*models.TrackedItem, error) {
	return s.itemRepo.GetByID(ctx, id)
}

func (s *inventoryService) GetItemsByPurpose(ctx context.Context, purpose string) ([]models.TrackedItem, error) {
	return s.itemRepo.GetItemsByPurpose(ctx, purpose)
}

// GetInventoryForSaleConfirm returns all INVENTORY items plus any items already
// attached to the sale, so the confirm form can show current attachments as
// pre-checked without losing them if they've left the raw inventory pool.
func (s *inventoryService) GetInventoryForSaleConfirm(ctx context.Context, sale *models.Sale) ([]models.TrackedItem, error) {
	items, err := s.itemRepo.GetItemsByPurpose(ctx, "INVENTORY")
	if err != nil {
		return nil, err
	}
	return append(items, sale.Items...), nil
}

func (s *inventoryService) UpdateItem(ctx context.Context, item *models.TrackedItem) error {
	return s.itemRepo.Update(ctx, item)
}

func (s *inventoryService) DeleteItem(ctx context.Context, id uint) error {
	return s.itemRepo.Delete(ctx, id)
}

func nullStr(s string) sql.NullString {
	if s == "" {
		return sql.NullString{}
	}
	return sql.NullString{String: s, Valid: true}
}
