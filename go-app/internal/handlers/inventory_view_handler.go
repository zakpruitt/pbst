package handlers

import (
	"fmt"
	"html/template"
	"net/http"
	"strconv"

	"github.com/zakpruitt/pbst/internal/models"
	"github.com/zakpruitt/pbst/internal/services"
)

type InventoryViewHandler struct {
	index        *template.Template
	new          *template.Template
	edit         *template.Template
	rowPartial   *template.Template
	inventorySvc services.InventoryService
}

func NewInventoryViewHandler(inventorySvc services.InventoryService) *InventoryViewHandler {
	return &InventoryViewHandler{
		index:        parseTemplate("inventory/index"),
		new:          parseTemplate("inventory/new"),
		edit:         parseTemplate("inventory/edit"),
		rowPartial:   parsePartialTemplate("inventory/partials/row"),
		inventorySvc: inventorySvc,
	}
}

type inventoryRowPreset struct {
	Name            string
	ItemType        string
	CostBasis       float64
	MarketPrice     float64
	PokemonCardID   string
	SealedProductID string
	SetName         string
	CardNumber      string
	ImageURL        string
	GradingCompany  string
	Grade           string
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

	purpose := r.FormValue("purpose")
	if purpose == "" {
		purpose = "INVENTORY"
	}

	input := services.CreateItemsInput{
		SnapshotJSON:    r.FormValue("items_snapshot"),
		Purpose:         purpose,
		AcquisitionDate: parseFormDate(r, "acquisition_date"),
	}
	if err := h.inventorySvc.CreateItems(r.Context(), input); err != nil {
		serverError(w, err)
		return
	}

	http.Redirect(w, r, "/inventory?purpose="+purpose, http.StatusSeeOther)
}

func (h *InventoryViewHandler) Inventory(w http.ResponseWriter, r *http.Request) {
	purpose := queryParam(r, "purpose", "INVENTORY")
	items, err := h.inventorySvc.GetItemsByPurpose(r.Context(), purpose)
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
		execTemplate(w, h.index, "inventory-page", data)
		return
	}
	execTemplate(w, h.index, "layout", data)
}

func (h *InventoryViewHandler) RowPartial(w http.ResponseWriter, r *http.Request) {
	q := r.URL.Query()
	itemType := q.Get("type")
	if itemType == "" {
		itemType = "OTHER"
	}
	market, _ := strconv.ParseFloat(q.Get("market"), 64)
	preset := inventoryRowPreset{
		Name:            q.Get("name"),
		ItemType:        itemType,
		MarketPrice:     market,
		PokemonCardID:   q.Get("card_id"),
		SealedProductID: q.Get("sealed_id"),
		SetName:         q.Get("set"),
		CardNumber:      q.Get("card"),
		ImageURL:        q.Get("img"),
	}
	execTemplate(w, h.rowPartial, "inventory-row", preset)
}

func (h *InventoryViewHandler) InventoryEditForm(w http.ResponseWriter, r *http.Request, id uint) {
	item, err := h.inventorySvc.GetItemByID(r.Context(), id)
	if err != nil {
		serverError(w, err)
		return
	}
	execTemplate(w, h.edit, "layout", map[string]any{
		"Page": "inventory",
		"Item": item,
	})
}

func (h *InventoryViewHandler) UpdateInventoryItem(w http.ResponseWriter, r *http.Request, id uint) {
	if err := r.ParseForm(); err != nil {
		serverError(w, err)
		return
	}
	item, err := h.inventorySvc.GetItemByID(r.Context(), id)
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

	if err := h.inventorySvc.UpdateItem(r.Context(), item); err != nil {
		serverError(w, err)
		return
	}
	http.Redirect(w, r, fmt.Sprintf("/inventory?purpose=%s", item.Purpose), http.StatusSeeOther)
}

func (h *InventoryViewHandler) DeleteInventoryItem(w http.ResponseWriter, r *http.Request, id uint) {
	if err := h.inventorySvc.DeleteItem(r.Context(), id); err != nil {
		serverError(w, err)
		return
	}
	http.Redirect(w, r, "/inventory", http.StatusSeeOther)
}
