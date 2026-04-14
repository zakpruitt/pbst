package handlers

import (
	"fmt"
	"html/template"
	"net/http"

	"github.com/zakpruitt/pbst/internal/models"
	"github.com/zakpruitt/pbst/internal/repository"
)

type InventoryViewHandler struct {
	index    *template.Template
	new      *template.Template
	edit     *template.Template
	itemRepo *repository.TrackedItemRepository
}

func NewInventoryViewHandler(itemRepo *repository.TrackedItemRepository) *InventoryViewHandler {
	return &InventoryViewHandler{
		index:    parseTemplate("inventory/index"),
		new:      parseTemplate("inventory/new"),
		edit:     parseTemplate("inventory/edit"),
		itemRepo: itemRepo,
	}
}

func (h *InventoryViewHandler) Inventory(w http.ResponseWriter, r *http.Request) {
	purpose := r.URL.Query().Get("purpose")
	if purpose == "" {
		purpose = "INVENTORY"
	}
	items, err := h.itemRepo.GetItemsByPurpose(r.Context(), purpose)
	if err != nil {
		serverError(w, err)
		return
	}
	raw, graded, sealed, other := groupInventoryByType(items)
	data := map[string]any{
		"Page":        "inventory",
		"Items":       items,
		"RawItems":    raw,
		"GradedItems": graded,
		"SealedItems": sealed,
		"OtherItems":  other,
		"Purpose":     purpose,
	}
	if r.Header.Get("HX-Request") != "" {
		execTemplate(w, h.index, "inventory-list", data)
		return
	}
	execTemplate(w, h.index, "layout", data)
}

func (h *InventoryViewHandler) InventoryNew(w http.ResponseWriter, r *http.Request) {
	execTemplate(w, h.new, "layout", map[string]any{
		"Page": "inventory",
	})
}

func (h *InventoryViewHandler) CreateInventoryItem(w http.ResponseWriter, r *http.Request) {
	if err := r.ParseForm(); err != nil {
		serverError(w, err)
		return
	}
	item := &models.TrackedItem{
		ManualNameOverride:    nullString(r.FormValue("name")),
		CostBasis:             parseFormFloat(r, "cost_basis"),
		MarketValueAtPurchase: parseFormFloat(r, "market_value"),
		AcquisitionDate:       parseFormDate(r, "acquisition_date"),
		Notes:                 nullString(r.FormValue("notes")),
		Purpose:               r.FormValue("purpose"),
		ItemType:              r.FormValue("item_type"),
	}
	if item.Purpose == "" {
		item.Purpose = "INVENTORY"
	}
	if item.ItemType == "" {
		item.ItemType = "OTHER"
	}
	if item.ItemType == "GRADED_CARD" {
		item.GradedDetails = &models.GradedDetails{
			GradingCompany: r.FormValue("grading_company"),
			Grade:          r.FormValue("grade"),
		}
	}
	if err := h.itemRepo.AddTrackedItem(r.Context(), item); err != nil {
		serverError(w, err)
		return
	}
	http.Redirect(w, r, "/inventory?purpose="+item.Purpose, http.StatusSeeOther)
}

func (h *InventoryViewHandler) InventoryEditForm(w http.ResponseWriter, r *http.Request) {
	id, ok := requirePathID(w, r)
	if !ok {
		return
	}
	item, err := h.itemRepo.GetByID(r.Context(), id)
	if err != nil {
		serverError(w, err)
		return
	}
	execTemplate(w, h.edit, "layout", map[string]any{
		"Page": "inventory",
		"Item": item,
	})
}

func (h *InventoryViewHandler) UpdateInventoryItem(w http.ResponseWriter, r *http.Request) {
	id, ok := requirePathID(w, r)
	if !ok {
		return
	}
	if err := r.ParseForm(); err != nil {
		serverError(w, err)
		return
	}
	item, err := h.itemRepo.GetByID(r.Context(), id)
	if err != nil {
		serverError(w, err)
		return
	}

	if v := r.FormValue("name"); v != "" {
		item.ManualNameOverride = nullString(v)
	}
	item.CostBasis = parseFormFloat(r, "cost_basis")
	item.MarketValueAtPurchase = parseFormFloat(r, "market_value")
	if d := parseFormDate(r, "acquisition_date"); !d.IsZero() {
		item.AcquisitionDate = d
	}
	item.Notes = nullString(r.FormValue("notes"))
	if p := r.FormValue("purpose"); p != "" {
		item.Purpose = p
	}

	if item.ItemType == "GRADED_CARD" {
		if item.GradedDetails == nil {
			item.GradedDetails = &models.GradedDetails{}
		}
		item.GradedDetails.GradingCompany = r.FormValue("grading_company")
		item.GradedDetails.Grade = r.FormValue("grade")
	}

	if err := h.itemRepo.Update(r.Context(), item); err != nil {
		serverError(w, err)
		return
	}
	http.Redirect(w, r, fmt.Sprintf("/inventory?purpose=%s", item.Purpose), http.StatusSeeOther)
}

func (h *InventoryViewHandler) DeleteInventoryItem(w http.ResponseWriter, r *http.Request) {
	id, ok := requirePathID(w, r)
	if !ok {
		return
	}
	if err := h.itemRepo.Delete(r.Context(), id); err != nil {
		serverError(w, err)
		return
	}
	http.Redirect(w, r, "/inventory", http.StatusSeeOther)
}

func groupInventoryByType(items []models.TrackedItem) (raw, graded, sealed, other []models.TrackedItem) {
	for _, item := range items {
		switch item.ItemType {
		case "SEALED_PRODUCT":
			sealed = append(sealed, item)
		case "GRADED_CARD":
			graded = append(graded, item)
		case "OTHER":
			other = append(other, item)
		default:
			raw = append(raw, item)
		}
	}
	return
}
