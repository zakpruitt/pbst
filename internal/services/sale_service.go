package services

import (
	"context"
	"fmt"

	"github.com/zakpruitt/pbst/internal/repository"
)

type SaleService struct {
	saleRepo *repository.SaleRepository
	itemRepo *repository.TrackedItemRepository
}

func NewSaleService(saleRepo *repository.SaleRepository, itemRepo *repository.TrackedItemRepository) *SaleService {
	return &SaleService{saleRepo: saleRepo, itemRepo: itemRepo}
}

// ConfirmWithItems attaches the given inventory items to the sale, marks them
// SOLD, and promotes the sale to CONFIRMED.
func (s *SaleService) ConfirmWithItems(ctx context.Context, saleID uint, itemIDs []uint) error {
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

// Ignore marks a sale as not-mine. Any attached items are released back to
// inventory and any prior attribution (e.g. Vince) is cleared so an accidental
// confirm can be undone cleanly.
func (s *SaleService) Ignore(ctx context.Context, saleID uint) error {
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
func (s *SaleService) MarkAsVince(ctx context.Context, saleID uint) error {
	if err := s.itemRepo.DetachFromSale(ctx, saleID); err != nil {
		return fmt.Errorf("detach items: %w", err)
	}
	if err := s.saleRepo.UpdateStatusAndAttribution(ctx, saleID, "IGNORED", "vince"); err != nil {
		return fmt.Errorf("mark sale as vince: %w", err)
	}
	return nil
}

// Unstage returns a sale to the triage pool, releases its items, and clears
// any prior attribution.
func (s *SaleService) Unstage(ctx context.Context, saleID uint) error {
	if err := s.itemRepo.DetachFromSale(ctx, saleID); err != nil {
		return fmt.Errorf("detach items: %w", err)
	}
	if err := s.saleRepo.UpdateStatusAndAttribution(ctx, saleID, "STAGED", ""); err != nil {
		return fmt.Errorf("unstage sale: %w", err)
	}
	return nil
}

// Delete releases any attached items back to inventory and then deletes the
// sale.
func (s *SaleService) Delete(ctx context.Context, saleID uint) error {
	if err := s.itemRepo.DetachFromSale(ctx, saleID); err != nil {
		return fmt.Errorf("detach items: %w", err)
	}
	if err := s.saleRepo.Delete(ctx, saleID); err != nil {
		return fmt.Errorf("delete sale: %w", err)
	}
	return nil
}
