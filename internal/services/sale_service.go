package services

import (
	"context"
	"fmt"

	"github.com/zakpruitt/pbst/internal/models"
	"github.com/zakpruitt/pbst/internal/repository"
)

type SaleService interface {
	CreateSale(ctx context.Context, sale *models.Sale) error
	GetSaleByID(ctx context.Context, id uint) (*models.Sale, error)
	GetAllSales(ctx context.Context, view string) ([]models.Sale, error)
	GetStagedSales(ctx context.Context) ([]models.Sale, error)
	CountStagedSales(ctx context.Context) (int64, error)
	ConfirmWithItems(ctx context.Context, saleID uint, itemIDs []uint) error
	Ignore(ctx context.Context, saleID uint) error
	MarkAsVince(ctx context.Context, saleID uint) error
	Unstage(ctx context.Context, saleID uint) error
	Delete(ctx context.Context, saleID uint) error
}

type saleService struct {
	saleRepo repository.SaleRepository
	itemRepo repository.TrackedItemRepository
}

func NewSaleService(saleRepo repository.SaleRepository, itemRepo repository.TrackedItemRepository) SaleService {
	return &saleService{saleRepo: saleRepo, itemRepo: itemRepo}
}

func (s *saleService) CreateSale(ctx context.Context, sale *models.Sale) error {
	return s.saleRepo.CreateSale(ctx, sale)
}

func (s *saleService) GetSaleByID(ctx context.Context, id uint) (*models.Sale, error) {
	return s.saleRepo.GetByID(ctx, id)
}

func (s *saleService) GetAllSales(ctx context.Context, view string) ([]models.Sale, error) {
	return s.saleRepo.GetAllSales(ctx, view)
}

func (s *saleService) GetStagedSales(ctx context.Context) ([]models.Sale, error) {
	return s.saleRepo.GetByStatus(ctx, "STAGED")
}

func (s *saleService) CountStagedSales(ctx context.Context) (int64, error) {
	return s.saleRepo.CountByStatus(ctx, "STAGED")
}

func (s *saleService) ConfirmWithItems(ctx context.Context, saleID uint, itemIDs []uint) error {
	if err := s.itemRepo.DetachFromSale(ctx, saleID); err != nil {
		return fmt.Errorf("detach existing items: %w", err)
	}
	if err := s.itemRepo.AttachToSale(ctx, itemIDs, saleID); err != nil {
		return fmt.Errorf("attach items: %w", err)
	}
	if err := s.saleRepo.UpdateStatus(ctx, saleID, "CONFIRMED"); err != nil {
		return fmt.Errorf("confirm sale: %w", err)
	}
	return nil
}

func (s *saleService) Ignore(ctx context.Context, saleID uint) error {
	if err := s.itemRepo.DetachFromSale(ctx, saleID); err != nil {
		return fmt.Errorf("detach items: %w", err)
	}
	if err := s.saleRepo.UpdateStatusAndAttribution(ctx, saleID, "IGNORED", ""); err != nil {
		return fmt.Errorf("ignore sale: %w", err)
	}
	return nil
}

// MarkAsVince tags a sale as belonging to Vince. Stored as IGNORED so every
// KPI query (which filters on CONFIRMED) already excludes it, plus the
// attributed_to flag lets us surface his sales on a dedicated tab.
func (s *saleService) MarkAsVince(ctx context.Context, saleID uint) error {
	if err := s.itemRepo.DetachFromSale(ctx, saleID); err != nil {
		return fmt.Errorf("detach items: %w", err)
	}
	if err := s.saleRepo.UpdateStatusAndAttribution(ctx, saleID, "IGNORED", "vince"); err != nil {
		return fmt.Errorf("mark sale as vince: %w", err)
	}
	return nil
}

func (s *saleService) Unstage(ctx context.Context, saleID uint) error {
	if err := s.itemRepo.DetachFromSale(ctx, saleID); err != nil {
		return fmt.Errorf("detach items: %w", err)
	}
	if err := s.saleRepo.UpdateStatusAndAttribution(ctx, saleID, "STAGED", ""); err != nil {
		return fmt.Errorf("unstage sale: %w", err)
	}
	return nil
}

func (s *saleService) Delete(ctx context.Context, saleID uint) error {
	if err := s.itemRepo.DetachFromSale(ctx, saleID); err != nil {
		return fmt.Errorf("detach items: %w", err)
	}
	if err := s.saleRepo.Delete(ctx, saleID); err != nil {
		return fmt.Errorf("delete sale: %w", err)
	}
	return nil
}
