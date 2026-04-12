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

type GradingViewHandler struct {
	grading       *template.Template
	gradingNew    *template.Template
	gradingDetail *template.Template
	gradingEdit   *template.Template
	gradingRepo   *repository.GradingRepository
	itemRepo      *repository.TrackedItemRepository
	gradingSvc    *services.GradingService
}

func NewGradingViewHandler(
	gradingRepo *repository.GradingRepository,
	itemRepo *repository.TrackedItemRepository,
	gradingSvc *services.GradingService,
) *GradingViewHandler {
	return &GradingViewHandler{
		grading:       parseTemplate("grading/index"),
		gradingNew:    parseTemplate("grading/new"),
		gradingDetail: parseTemplate("grading/detail"),
		gradingEdit:   parseTemplate("grading/edit"),
		gradingRepo:   gradingRepo,
		itemRepo:      itemRepo,
		gradingSvc:    gradingSvc,
	}
}
func (h *GradingViewHandler) GradingNew(w http.ResponseWriter, r *http.Request) {
	items, err := h.itemRepo.GetInventoryItems(r.Context())
	if err != nil {
		serverError(w, err)
		return
	}

	raw, graded := splitInventoryItems(items)
	execTemplate(w, h.gradingNew, "layout", map[string]any{
		"Page":        "grading",
		"RawItems":    raw,
		"GradedItems": graded,
	})
}

func (h *GradingViewHandler) CreateGrading(w http.ResponseWriter, r *http.Request) {
	if err := r.ParseForm(); err != nil {
		serverError(w, err)
		return
	}

	costPerCard := parseFormFloat(r, "cost_per_card")
	itemIDs := parseFormIDs(r.Form, "item_ids")
	notes := nullString(r.FormValue("notes"))

	submission, err := h.gradingSvc.CreateWithItems(
		r.Context(),
		r.FormValue("company"),
		r.FormValue("submission_method"),
		costPerCard,
		notes,
		itemIDs,
	)
	if err != nil {
		serverError(w, err)
		return
	}

	http.Redirect(w, r, fmt.Sprintf("/grading/%d", submission.ID), http.StatusSeeOther)
}

func (h *GradingViewHandler) Grading(w http.ResponseWriter, r *http.Request) {
	submissions, err := h.gradingRepo.GetAllSubmissions(r.Context())
	if err != nil {
		serverError(w, err)
		return
	}

	var active, returned []models.GradingSubmission
	for _, s := range submissions {
		if s.Status == "RETURNED" {
			returned = append(returned, s)
		} else {
			active = append(active, s)
		}
	}

	execTemplate(w, h.grading, "layout", map[string]any{
		"Page":     "grading",
		"Active":   active,
		"Returned": returned,
	})
}

func (h *GradingViewHandler) GradingDetail(w http.ResponseWriter, r *http.Request) {
	id, ok := requirePathID(w, r)
	if !ok {
		return
	}

	submission, err := h.gradingRepo.GetSubmissionByID(r.Context(), id)
	if err != nil {
		serverError(w, err)
		return
	}

	execTemplate(w, h.gradingDetail, "layout", map[string]any{
		"Page":       "grading",
		"Submission": submission,
	})
}
func (h *GradingViewHandler) GradingEditForm(w http.ResponseWriter, r *http.Request) {
	id, ok := requirePathID(w, r)
	if !ok {
		return
	}

	submission, err := h.gradingRepo.GetSubmissionByID(r.Context(), id)
	if err != nil {
		serverError(w, err)
		return
	}

	attachedIDs := make(map[uint]bool)
	for _, item := range submission.Items {
		attachedIDs[item.ID] = true
	}

	inventoryItems, err := h.itemRepo.GetInventoryItems(r.Context())
	if err != nil {
		serverError(w, err)
		return
	}

	allItems := append(inventoryItems, submission.Items...)
	raw, graded := splitInventoryItems(allItems)

	execTemplate(w, h.gradingEdit, "layout", map[string]any{
		"Page":        "grading",
		"Submission":  submission,
		"RawItems":    raw,
		"GradedItems": graded,
		"AttachedIDs": attachedIDs,
	})
}

func (h *GradingViewHandler) UpdateGrading(w http.ResponseWriter, r *http.Request) {
	id, ok := requirePathID(w, r)
	if !ok {
		return
	}

	if err := r.ParseForm(); err != nil {
		serverError(w, err)
		return
	}

	costPerCard := parseFormFloat(r, "cost_per_card")
	itemIDs := parseFormIDs(r.Form, "item_ids")
	notes := nullString(r.FormValue("notes"))

	err := h.gradingSvc.UpdateSubmission(
		r.Context(),
		id,
		r.FormValue("company"),
		r.FormValue("submission_method"),
		costPerCard,
		notes,
		itemIDs,
	)
	if err != nil {
		serverError(w, err)
		return
	}

	http.Redirect(w, r, fmt.Sprintf("/grading/%d", id), http.StatusSeeOther)
}

func (h *GradingViewHandler) AdvanceGradingStatus(w http.ResponseWriter, r *http.Request) {
	id, ok := requirePathID(w, r)
	if !ok {
		return
	}

	if err := h.gradingSvc.AdvanceStatus(r.Context(), id, r.FormValue("new_status")); err != nil {
		serverError(w, err)
		return
	}

	http.Redirect(w, r, fmt.Sprintf("/grading/%d", id), http.StatusSeeOther)
}

func (h *GradingViewHandler) RecordReturn(w http.ResponseWriter, r *http.Request) {
	id, ok := requirePathID(w, r)
	if !ok {
		return
	}

	if err := r.ParseForm(); err != nil {
		serverError(w, err)
		return
	}

	var grades []services.ItemGrade
	for _, s := range r.Form["item_ids"] {
		itemID, err := strconv.ParseUint(s, 10, 64)
		if err != nil {
			continue
		}
		grade := r.FormValue(fmt.Sprintf("grade_%d", itemID))
		upcharge, _ := strconv.ParseFloat(r.FormValue(fmt.Sprintf("upcharge_%d", itemID)), 64)
		grades = append(grades, services.ItemGrade{
			ItemID:   uint(itemID),
			Grade:    grade,
			Upcharge: upcharge,
		})
	}

	if err := h.gradingSvc.RecordReturn(r.Context(), id, grades); err != nil {
		serverError(w, err)
		return
	}

	http.Redirect(w, r, fmt.Sprintf("/grading/%d", id), http.StatusSeeOther)
}
func splitInventoryItems(items []models.TrackedItem) (raw, graded []models.TrackedItem) {
	for _, item := range items {
		if item.GradedDetails != nil && item.GradedDetails.GradingCompany != "" {
			graded = append(graded, item)
		} else {
			raw = append(raw, item)
		}
	}
	return
}
