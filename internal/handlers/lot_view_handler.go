package handlers

import (
	"encoding/json"
	"fmt"
	"html/template"
	"net/http"
	"strconv"
	"time"

	"github.com/zakpruitt/pbst/internal/models"
	"github.com/zakpruitt/pbst/internal/repository"
	"github.com/zakpruitt/pbst/internal/services"
)

type LotViewHandler struct {
	lots      *template.Template
	lotDetail *template.Template
	lotNew    *template.Template
	lotEdit   *template.Template
	lotRepo   *repository.LotRepository
	lotSvc    *services.LotService
}

func NewLotViewHandler(
	lotRepo *repository.LotRepository,
	lotSvc *services.LotService,
) *LotViewHandler {
	return &LotViewHandler{
		lots:      parseTemplate("lots"),
		lotDetail: parseTemplate("lot-detail"),
		lotNew:    parseTemplate("lot-new"),
		lotEdit:   parseTemplate("lot-edit"),
		lotRepo:   lotRepo,
		lotSvc:    lotSvc,
	}
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
	id, err := parsePathID(r, "id")
	if err != nil {
		http.NotFound(w, r)
		return
	}
	lot, err := h.lotRepo.GetLotWithItems(r.Context(), id)
	if err != nil {
		serverError(w, err)
		return
	}

	var snapshot []models.SnapshotItem
	if lot.LotContentSnapshot != "" {
		if err := json.Unmarshal([]byte(lot.LotContentSnapshot), &snapshot); err != nil {
			serverError(w, err)
			return
		}
	}

	execTemplate(w, h.lotDetail, "layout", map[string]any{
		"Page":          "lots",
		"Lot":           lot,
		"SnapshotItems": snapshot,
	})
}

func (h *LotViewHandler) LotNew(w http.ResponseWriter, r *http.Request) {
	execTemplate(w, h.lotNew, "layout", map[string]any{"Page": "lots"})
}

func (h *LotViewHandler) LotEditForm(w http.ResponseWriter, r *http.Request) {
	id, err := parsePathID(r, "id")
	if err != nil {
		http.NotFound(w, r)
		return
	}
	lot, err := h.lotRepo.GetLotByID(r.Context(), id)
	if err != nil {
		serverError(w, err)
		return
	}

	snapshotJSON := template.JS("[]")
	if lot.LotContentSnapshot != "" {
		snapshotJSON = template.JS(lot.LotContentSnapshot)
	}

	execTemplate(w, h.lotEdit, "layout", map[string]any{
		"Page":         "lots",
		"Lot":          lot,
		"SnapshotJSON": snapshotJSON,
	})
}

func (h *LotViewHandler) SaveLot(w http.ResponseWriter, r *http.Request) {
	totalCost, _ := strconv.ParseFloat(r.FormValue("total_cost"), 64)
	emv, _ := strconv.ParseFloat(r.FormValue("estimated_market_value"), 64)
	purchaseDate, _ := time.Parse("2006-01-02", r.FormValue("purchase_date"))

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

func (h *LotViewHandler) UpdateLot(w http.ResponseWriter, r *http.Request) {
	id, err := parsePathID(r, "id")
	if err != nil {
		http.NotFound(w, r)
		return
	}
	lot, err := h.lotRepo.GetLotByID(r.Context(), id)
	if err != nil {
		serverError(w, err)
		return
	}

	totalCost, _ := strconv.ParseFloat(r.FormValue("total_cost"), 64)
	emv, _ := strconv.ParseFloat(r.FormValue("estimated_market_value"), 64)
	purchaseDate, _ := time.Parse("2006-01-02", r.FormValue("purchase_date"))

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

func (h *LotViewHandler) UpdateLotStatus(w http.ResponseWriter, r *http.Request) {
	id, err := parsePathID(r, "id")
	if err != nil {
		http.NotFound(w, r)
		return
	}

	action := r.FormValue("action")
	switch action {
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
