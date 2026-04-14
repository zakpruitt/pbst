package handlers

import (
	"fmt"
	"html/template"
	"net/http"
	"time"

	"github.com/zakpruitt/pbst/internal/models"
	"github.com/zakpruitt/pbst/internal/repository"
	"github.com/zakpruitt/pbst/internal/services"
)

type SaleViewHandler struct {
	sales       *template.Template
	saleNew     *template.Template
	saleDetail  *template.Template
	saleStaging *template.Template
	saleConfirm *template.Template
	saleRepo    *repository.SaleRepository
	itemRepo    *repository.TrackedItemRepository
	saleSvc     *services.SaleService
}

type MonthGroup struct {
	Label    string
	FirstDay time.Time
	Sales    []models.Sale
}

func NewSaleViewHandler(
	saleRepo *repository.SaleRepository,
	itemRepo *repository.TrackedItemRepository,
	saleSvc *services.SaleService,
) *SaleViewHandler {
	return &SaleViewHandler{
		sales:       parseTemplate("sales/index"),
		saleNew:     parseTemplate("sales/new"),
		saleDetail:  parseTemplate("sales/detail"),
		saleStaging: parseTemplate("sales/staging"),
		saleConfirm: parseTemplate("sales/confirm"),
		saleRepo:    saleRepo,
		itemRepo:    itemRepo,
		saleSvc:     saleSvc,
	}
}

func (h *SaleViewHandler) Sales(w http.ResponseWriter, r *http.Request) {
	includeIgnored := r.URL.Query().Get("ignored") == "1"
	sales, err := h.saleRepo.GetAllSales(r.Context(), includeIgnored)
	if err != nil {
		serverError(w, err)
		return
	}

	stagedCount, err := h.saleRepo.CountByStatus(r.Context(), "STAGED")
	if err != nil {
		serverError(w, err)
		return
	}

	execTemplate(w, h.sales, "layout", map[string]any{
		"Page":           "sales",
		"Groups":         groupSalesByMonth(sales),
		"StagedCount":    stagedCount,
		"IncludeIgnored": includeIgnored,
	})
}

func (h *SaleViewHandler) SalesStaging(w http.ResponseWriter, r *http.Request) {
	sales, err := h.saleRepo.GetByStatus(r.Context(), "STAGED")
	if err != nil {
		serverError(w, err)
		return
	}
	execTemplate(w, h.saleStaging, "layout", map[string]any{
		"Page":  "sales",
		"Sales": sales,
	})
}

func (h *SaleViewHandler) SaleDetail(w http.ResponseWriter, r *http.Request) {
	id, ok := requirePathID(w, r)
	if !ok {
		return
	}
	sale, err := h.saleRepo.GetByID(r.Context(), id)
	if err != nil {
		http.NotFound(w, r)
		return
	}
	execTemplate(w, h.saleDetail, "layout", map[string]any{
		"Page": "sales",
		"Sale": sale,
	})
}

func (h *SaleViewHandler) SaleConfirmForm(w http.ResponseWriter, r *http.Request) {
	id, ok := requirePathID(w, r)
	if !ok {
		return
	}
	sale, err := h.saleRepo.GetByID(r.Context(), id)
	if err != nil {
		http.NotFound(w, r)
		return
	}

	attachedIDs := make(map[uint]bool)
	for _, item := range sale.Items {
		attachedIDs[item.ID] = true
	}

	inventoryItems, err := h.itemRepo.GetInventoryItems(r.Context())
	if err != nil {
		serverError(w, err)
		return
	}

	allItems := append(inventoryItems, sale.Items...)
	raw, graded := splitInventoryItems(allItems)

	execTemplate(w, h.saleConfirm, "layout", map[string]any{
		"Page":        "sales",
		"Sale":        sale,
		"RawItems":    raw,
		"GradedItems": graded,
		"AttachedIDs": attachedIDs,
	})
}

func (h *SaleViewHandler) ConfirmSale(w http.ResponseWriter, r *http.Request) {
	id, ok := requirePathID(w, r)
	if !ok {
		return
	}
	if err := r.ParseForm(); err != nil {
		serverError(w, err)
		return
	}
	itemIDs := parseFormIDs(r.Form, "item_ids")
	if err := h.saleSvc.ConfirmWithItems(r.Context(), id, itemIDs); err != nil {
		serverError(w, err)
		return
	}
	http.Redirect(w, r, fmt.Sprintf("/sales/%d", id), http.StatusSeeOther)
}

func (h *SaleViewHandler) IgnoreSale(w http.ResponseWriter, r *http.Request) {
	id, ok := requirePathID(w, r)
	if !ok {
		return
	}
	if err := h.saleSvc.Ignore(r.Context(), id); err != nil {
		serverError(w, err)
		return
	}
	http.Redirect(w, r, "/sales/staging", http.StatusSeeOther)
}

func (h *SaleViewHandler) DeleteSale(w http.ResponseWriter, r *http.Request) {
	id, ok := requirePathID(w, r)
	if !ok {
		return
	}
	if err := h.saleSvc.Delete(r.Context(), id); err != nil {
		serverError(w, err)
		return
	}
	http.Redirect(w, r, "/sales", http.StatusSeeOther)
}

func (h *SaleViewHandler) UnstageSale(w http.ResponseWriter, r *http.Request) {
	id, ok := requirePathID(w, r)
	if !ok {
		return
	}
	if err := h.saleSvc.Unstage(r.Context(), id); err != nil {
		serverError(w, err)
		return
	}
	http.Redirect(w, r, "/sales/staging", http.StatusSeeOther)
}

func (h *SaleViewHandler) SaleNew(w http.ResponseWriter, r *http.Request) {
	execTemplate(w, h.saleNew, "layout", map[string]any{"Page": "sales"})
}

func (h *SaleViewHandler) CreateSale(w http.ResponseWriter, r *http.Request) {
	gross := parseFormFloat(r, "gross_amount")
	fees := parseFormFloat(r, "ebay_fees")
	shipping := parseFormFloat(r, "shipping_cost")
	saleDate := parseFormDate(r, "sale_date")

	origin := r.FormValue("origin")
	if origin == "" {
		origin = "EBAY"
	}

	// Manual entries are Zak's by definition — skip the stager.
	sale := &models.Sale{
		EbayOrderID:   r.FormValue("ebay_order_id"),
		Title:         r.FormValue("title"),
		BuyerUsername: r.FormValue("buyer_username"),
		SaleDate:      saleDate,
		GrossAmount:   gross,
		EbayFees:      fees,
		ShippingCost:  shipping,
		NetAmount:     gross - fees - shipping,
		OrderStatus:   "COMPLETED",
		Origin:        origin,
		Status:        "CONFIRMED",
	}

	if err := h.saleRepo.CreateSale(r.Context(), sale); err != nil {
		serverError(w, err)
		return
	}

	http.Redirect(w, r, "/sales", http.StatusSeeOther)
}

// groupSalesByMonth buckets sales (assumed sorted by sale_date DESC) into
// contiguous month groups for display.
func groupSalesByMonth(sales []models.Sale) []MonthGroup {
	var groups []MonthGroup
	var current string
	for _, s := range sales {
		label := s.SaleDate.Format("January 2006")
		if label != current {
			groups = append(groups, MonthGroup{Label: label, FirstDay: s.SaleDate})
			current = label
		}
		i := len(groups) - 1
		groups[i].Sales = append(groups[i].Sales, s)
	}
	return groups
}
