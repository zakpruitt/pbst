package handlers

import (
	"log/slog"
	"net/http"

	"github.com/zakpruitt/pbst/internal/repository"
)

func NewDashboardHandler(
	lotRepo *repository.LotRepository,
	saleRepo *repository.SaleRepository,
	itemRepo *repository.TrackedItemRepository,
) http.HandlerFunc {
	tmpl := parseTemplate("dashboard")
	return func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/" {
			http.NotFound(w, r)
			return
		}
		ctx := r.Context()

		totalSpent, err := lotRepo.GetTotalCostNonRejected(ctx)
		if err != nil {
			slog.Error("dashboard: total spent", "error", err)
		}
		totalGross, err := saleRepo.GetTotalGrossAmount(ctx)
		if err != nil {
			slog.Error("dashboard: total gross", "error", err)
		}
		totalNet, err := saleRepo.GetTotalNetAmount(ctx)
		if err != nil {
			slog.Error("dashboard: total net", "error", err)
		}
		gradingCount, err := itemRepo.CountByPurpose(ctx, "IN_GRADING")
		if err != nil {
			slog.Error("dashboard: grading count", "error", err)
		}
		inventoryCount, err := itemRepo.CountByPurpose(ctx, "INVENTORY")
		if err != nil {
			slog.Error("dashboard: inventory count", "error", err)
		}

		execTemplate(w, tmpl, "layout", map[string]any{
			"Page":           "dashboard",
			"TotalSpent":     totalSpent,
			"TotalGross":     totalGross,
			"TotalNet":       totalNet,
			"Margin":         totalNet - totalSpent,
			"GradingCount":   gradingCount,
			"InventoryCount": inventoryCount,
		})
	}
}
