package handlers

import (
	"fmt"
	"html/template"
	"net/http"
	"strconv"

	"github.com/zakpruitt/pbst/internal/models"
	"github.com/zakpruitt/pbst/internal/repository"
	"github.com/zakpruitt/pbst/internal/services"
)

type LotViewHandler struct {
	lots       *template.Template
	lotNew     *template.Template
	lotDetail  *template.Template
	lotEdit    *template.Template
	rowPartial *template.Template
	lotRepo    *repository.LotRepository
	lotSvc     *services.LotService
	cardRepo   *repository.PokemonCardRepository
}

func NewLotViewHandler(
	lotRepo *repository.LotRepository,
	lotSvc *services.LotService,
	cardRepo *repository.PokemonCardRepository,
) *LotViewHandler {
	return &LotViewHandler{
		lots:       parseTemplate("lots/index"),
		lotNew:     parseTemplateWithPartial("lots/new", "lots/partials/row"),
		lotDetail:  parseTemplate("lots/detail"),
		lotEdit:    parseTemplateWithPartial("lots/edit", "lots/partials/row"),
		rowPartial: parsePartialTemplate("lots/partials/row"),
		lotRepo:    lotRepo,
		lotSvc:     lotSvc,
		cardRepo:   cardRepo,
	}
}
func (h *LotViewHandler) LotNew(w http.ResponseWriter, r *http.Request) {
	execTemplate(w, h.lotNew, "layout", map[string]any{"Page": "lots"})
}

func (h *LotViewHandler) SaveLot(w http.ResponseWriter, r *http.Request) {
	totalCost := parseFormFloat(r, "total_cost")
	emv := parseFormFloat(r, "estimated_market_value")
	purchaseDate := parseFormDate(r, "purchase_date")

	lot := &models.LotPurchase{
		SellerName:           r.FormValue("seller_name"),
		PurchaseDate:         purchaseDate,
		TotalCost:            totalCost,
		EstimatedMarketValue: emv,
		Description:          r.FormValue("description"),
		LotContentSnapshot:   r.FormValue("lot_content_snapshot"),
		Status:               "PENDING",
	}

	if err := h.lotRepo.CreateLot(r.Context(), lot); err != nil {
		serverError(w, err)
		return
	}

	http.Redirect(w, r, fmt.Sprintf("/lots/%d", lot.ID), http.StatusSeeOther)
}
func (h *LotViewHandler) Lots(w http.ResponseWriter, r *http.Request) {
	lots, err := h.lotRepo.GetAllLots(r.Context())
	if err != nil {
		serverError(w, err)
		return
	}
	execTemplate(w, h.lots, "layout", map[string]any{"Page": "lots", "Lots": lots})
}

func (h *LotViewHandler) LotDetail(w http.ResponseWriter, r *http.Request) {
	id, ok := requirePathID(w, r)
	if !ok {
		return
	}

	lot, err := h.lotRepo.GetLotWithItems(r.Context(), id)
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

// RowPartial renders a single lot item row as an HTML fragment.
// Used by the Alpine card-picker fetch and the "Add Other" button.
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
func (h *LotViewHandler) LotEditForm(w http.ResponseWriter, r *http.Request) {
	id, ok := requirePathID(w, r)
	if !ok {
		return
	}

	lot, err := h.lotRepo.GetLotByID(r.Context(), id)
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

func (h *LotViewHandler) UpdateLot(w http.ResponseWriter, r *http.Request) {
	id, ok := requirePathID(w, r)
	if !ok {
		return
	}

	lot, err := h.lotRepo.GetLotByID(r.Context(), id)
	if err != nil {
		serverError(w, err)
		return
	}

	totalCost := parseFormFloat(r, "total_cost")
	emv := parseFormFloat(r, "estimated_market_value")
	purchaseDate := parseFormDate(r, "purchase_date")

	lot.SellerName = r.FormValue("seller_name")
	lot.PurchaseDate = purchaseDate
	lot.TotalCost = totalCost
	lot.EstimatedMarketValue = emv
	lot.Description = r.FormValue("description")
	lot.LotContentSnapshot = r.FormValue("lot_content_snapshot")

	if err = h.lotRepo.UpdateLot(r.Context(), lot); err != nil {
		serverError(w, err)
		return
	}

	http.Redirect(w, r, fmt.Sprintf("/lots/%d", lot.ID), http.StatusSeeOther)
}

func (h *LotViewHandler) DeleteLot(w http.ResponseWriter, r *http.Request) {
	id, ok := requirePathID(w, r)
	if !ok {
		return
	}
	if err := h.lotSvc.DeleteLot(r.Context(), id); err != nil {
		serverError(w, err)
		return
	}
	http.Redirect(w, r, "/lots", http.StatusSeeOther)
}

func (h *LotViewHandler) UpdateLotStatus(w http.ResponseWriter, r *http.Request) {
	id, ok := requirePathID(w, r)
	if !ok {
		return
	}

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
