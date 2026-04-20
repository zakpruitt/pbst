package handlers

import (
	"fmt"
	"html/template"
	"net/http"
	"strconv"

	"github.com/zakpruitt/pbst/internal/models"
	"github.com/zakpruitt/pbst/internal/services"
)

type LotViewHandler struct {
	lots       *template.Template
	lotNew     *template.Template
	lotDetail  *template.Template
	lotEdit    *template.Template
	rowPartial *template.Template
	lotSvc     services.LotService
}

func NewLotViewHandler(lotSvc services.LotService) *LotViewHandler {
	return &LotViewHandler{
		lots:       parseTemplate("lots/index"),
		lotNew:     parseTemplateWithPartial("lots/new", "lots/partials/row"),
		lotDetail:  parseTemplate("lots/detail"),
		lotEdit:    parseTemplateWithPartial("lots/edit", "lots/partials/row"),
		rowPartial: parsePartialTemplate("lots/partials/row"),
		lotSvc:     lotSvc,
	}
}

func (h *LotViewHandler) LotNew(w http.ResponseWriter, r *http.Request) {
	execTemplate(w, h.lotNew, "layout", map[string]any{"Page": "lots"})
}

func (h *LotViewHandler) SaveLot(w http.ResponseWriter, r *http.Request) {
	lot := &models.LotPurchase{
		SellerName:           r.FormValue("seller_name"),
		PurchaseDate:         parseFormDate(r, "purchase_date"),
		TotalCost:            parseFormFloat(r, "total_cost"),
		EstimatedMarketValue: parseFormFloat(r, "estimated_market_value"),
		Description:          r.FormValue("description"),
		LotContentSnapshot:   r.FormValue("lot_content_snapshot"),
		Status:               "PENDING",
	}

	if err := h.lotSvc.CreateLot(r.Context(), lot); err != nil {
		serverError(w, err)
		return
	}

	http.Redirect(w, r, fmt.Sprintf("/lots/%d", lot.ID), http.StatusSeeOther)
}

func (h *LotViewHandler) Lots(w http.ResponseWriter, r *http.Request) {
	lots, err := h.lotSvc.GetAllLots(r.Context())
	if err != nil {
		serverError(w, err)
		return
	}
	execTemplate(w, h.lots, "layout", map[string]any{"Page": "lots", "Lots": lots})
}

func (h *LotViewHandler) LotDetail(w http.ResponseWriter, r *http.Request, id uint) {
	lot, err := h.lotSvc.GetLotWithItems(r.Context(), id)
	if err != nil {
		serverError(w, err)
		return
	}

	snapshot, err := lot.ParseSnapshot()
	if err != nil {
		serverError(w, err)
		return
	}

	execTemplate(w, h.lotDetail, "layout", map[string]any{
		"Page":          "lots",
		"Lot":           lot,
		"SnapshotItems": snapshot,
	})
}

func (h *LotViewHandler) RowPartial(w http.ResponseWriter, r *http.Request) {
	q := r.URL.Query()
	market := parseFormFloat(r, "market")
	qty, _ := strconv.Atoi(q.Get("qty"))
	if qty == 0 {
		qty = 1
	}

	itemType := q.Get("type")
	if itemType == "" {
		itemType = "RAW_CARD"
	}

	item := models.SnapshotItem{
		PokemonCardID: q.Get("card_id"),
		Name:          q.Get("name"),
		SetName:       q.Get("set"),
		CardNumber:    q.Get("card"),
		Rarity:        q.Get("rarity"),
		ImageURL:      q.Get("img"),
		ItemType:      itemType,
		Qty:           qty,
		MarketPrice:   market,
		Percentage:    60,
	}

	execTemplate(w, h.rowPartial, "lot-row", item)
}

func (h *LotViewHandler) LotEditForm(w http.ResponseWriter, r *http.Request, id uint) {
	lot, err := h.lotSvc.GetLotByID(r.Context(), id)
	if err != nil {
		serverError(w, err)
		return
	}

	items, err := lot.ParseSnapshot()
	if err != nil {
		serverError(w, err)
		return
	}

	execTemplate(w, h.lotEdit, "layout", map[string]any{
		"Page":          "lots",
		"Lot":           lot,
		"SnapshotItems": items,
	})
}

func (h *LotViewHandler) UpdateLot(w http.ResponseWriter, r *http.Request, id uint) {
	lot, err := h.lotSvc.GetLotByID(r.Context(), id)
	if err != nil {
		serverError(w, err)
		return
	}

	lot.SellerName = r.FormValue("seller_name")
	lot.PurchaseDate = parseFormDate(r, "purchase_date")
	lot.TotalCost = parseFormFloat(r, "total_cost")
	lot.EstimatedMarketValue = parseFormFloat(r, "estimated_market_value")
	lot.Description = r.FormValue("description")
	lot.LotContentSnapshot = r.FormValue("lot_content_snapshot")

	if err = h.lotSvc.UpdateLot(r.Context(), lot); err != nil {
		serverError(w, err)
		return
	}

	http.Redirect(w, r, fmt.Sprintf("/lots/%d", lot.ID), http.StatusSeeOther)
}

func (h *LotViewHandler) DeleteLot(w http.ResponseWriter, r *http.Request, id uint) {
	if err := h.lotSvc.DeleteLot(r.Context(), id); err != nil {
		serverError(w, err)
		return
	}
	http.Redirect(w, r, "/lots", http.StatusSeeOther)
}

func (h *LotViewHandler) UpdateLotStatus(w http.ResponseWriter, r *http.Request, id uint) {
	var err error
	switch r.FormValue("action") {
	case "accept":
		err = h.lotSvc.AcceptLot(r.Context(), id)
	case "reject":
		err = h.lotSvc.RejectLot(r.Context(), id)
	}
	if err != nil {
		serverError(w, err)
		return
	}
	http.Redirect(w, r, fmt.Sprintf("/lots/%d", id), http.StatusSeeOther)
}
