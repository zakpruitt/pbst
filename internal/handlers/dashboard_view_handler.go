package handlers

import (
	"html/template"
	"net/http"

	"github.com/zakpruitt/pbst/internal/repository"
)

type DashboardViewHandler struct {
	tmpl     *template.Template
	lotRepo  *repository.LotRepository
	saleRepo *repository.SaleRepository
	itemRepo *repository.TrackedItemRepository
}

func NewDashboardViewHandler(
	lotRepo *repository.LotRepository,
	saleRepo *repository.SaleRepository,
	itemRepo *repository.TrackedItemRepository,
) *DashboardViewHandler {
	return &DashboardViewHandler{
		tmpl:     parseTemplate("dashboard"),
		lotRepo:  lotRepo,
		saleRepo: saleRepo,
		itemRepo: itemRepo,
	}
}

func (h *DashboardViewHandler) Dashboard(w http.ResponseWriter, r *http.Request) {
	if r.URL.Path != "/" {
		http.NotFound(w, r)
		return
	}
	ctx := r.Context()
	totalSpent, _ := h.lotRepo.GetTotalCostNonRejected(ctx)
	totalGross, _ := h.saleRepo.GetTotalGrossAmount(ctx)
	totalNet, _ := h.saleRepo.GetTotalNetAmount(ctx)
	pcCount, _ := h.itemRepo.CountByPurpose(ctx, "PERSONAL_COLLECTION")
	gradingCount, _ := h.itemRepo.CountByPurpose(ctx, "IN_GRADING")
	inventoryCount, _ := h.itemRepo.CountByPurpose(ctx, "GRADED_INVENTORY")

	execTemplate(w, h.tmpl, "layout", map[string]any{
		"Page":           "dashboard",
		"TotalSpent":     totalSpent,
		"TotalGross":     totalGross,
		"TotalNet":       totalNet,
		"Margin":         totalNet - totalSpent,
		"PCCount":        pcCount,
		"GradingCount":   gradingCount,
		"InventoryCount": inventoryCount,
	})
}
