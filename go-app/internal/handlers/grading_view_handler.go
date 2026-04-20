package handlers

import (
	"fmt"
	"html/template"
	"net/http"
	"strconv"
	"time"

	"github.com/zakpruitt/pbst/internal/models"
	"github.com/zakpruitt/pbst/internal/services"
)

type GradingViewHandler struct {
	grading       *template.Template
	gradingNew    *template.Template
	gradingDetail *template.Template
	gradingEdit   *template.Template
	gradingSvc    services.GradingService
	inventorySvc  services.InventoryService
}

func NewGradingViewHandler(
	gradingSvc services.GradingService,
	inventorySvc services.InventoryService,
) *GradingViewHandler {
	return &GradingViewHandler{
		grading:       parseTemplate("grading/index"),
		gradingNew:    parseTemplate("grading/new"),
		gradingDetail: parseTemplate("grading/detail"),
		gradingEdit:   parseTemplate("grading/edit"),
		gradingSvc:    gradingSvc,
		inventorySvc:  inventorySvc,
	}
}

func (h *GradingViewHandler) GradingNew(w http.ResponseWriter, r *http.Request) {
	items, err := h.inventorySvc.GetItemsByPurpose(r.Context(), "INVENTORY")
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

	submissionCost := parseFormFloat(r, "submission_cost")
	itemIDs := parseFormIDs(r.Form, "item_ids")
	notes := nullString(r.FormValue("notes"))

	submission, err := h.gradingSvc.CreateWithItems(
		r.Context(),
		r.FormValue("company"),
		r.FormValue("submission_method"),
		submissionCost,
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
	submissions, err := h.gradingSvc.GetAllSubmissions(r.Context())
	if err != nil {
		serverError(w, err)
		return
	}

	groups := groupByMonth(submissions, func(s models.GradingSubmission) time.Time { return s.CreatedAt })
	execTemplate(w, h.grading, "layout", map[string]any{
		"Page":   "grading",
		"Groups": groups,
	})
}

func (h *GradingViewHandler) GradingDetail(w http.ResponseWriter, r *http.Request, id uint) {
	submission, err := h.gradingSvc.GetSubmissionByID(r.Context(), id)
	if err != nil {
		serverError(w, err)
		return
	}

	execTemplate(w, h.gradingDetail, "layout", map[string]any{
		"Page":       "grading",
		"Submission": submission,
	})
}

func (h *GradingViewHandler) GradingEditForm(w http.ResponseWriter, r *http.Request, id uint) {
	submission, err := h.gradingSvc.GetSubmissionByID(r.Context(), id)
	if err != nil {
		serverError(w, err)
		return
	}

	attachedIDs := make(map[uint]bool)
	for _, item := range submission.Items {
		attachedIDs[item.ID] = true
	}

	inventoryItems, err := h.inventorySvc.GetItemsByPurpose(r.Context(), "INVENTORY")
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

func (h *GradingViewHandler) UpdateGrading(w http.ResponseWriter, r *http.Request, id uint) {
	if err := r.ParseForm(); err != nil {
		serverError(w, err)
		return
	}

	submissionCost := parseFormFloat(r, "submission_cost")
	itemIDs := parseFormIDs(r.Form, "item_ids")
	notes := nullString(r.FormValue("notes"))

	err := h.gradingSvc.UpdateSubmission(
		r.Context(),
		id,
		r.FormValue("company"),
		r.FormValue("submission_method"),
		submissionCost,
		notes,
		itemIDs,
	)
	if err != nil {
		serverError(w, err)
		return
	}

	http.Redirect(w, r, fmt.Sprintf("/grading/%d", id), http.StatusSeeOther)
}

func (h *GradingViewHandler) DeleteGrading(w http.ResponseWriter, r *http.Request, id uint) {
	if err := h.gradingSvc.DeleteSubmission(r.Context(), id); err != nil {
		serverError(w, err)
		return
	}
	http.Redirect(w, r, "/grading", http.StatusSeeOther)
}

func (h *GradingViewHandler) AdvanceGradingStatus(w http.ResponseWriter, r *http.Request, id uint) {
	if err := h.gradingSvc.AdvanceStatus(r.Context(), id, r.FormValue("new_status")); err != nil {
		serverError(w, err)
		return
	}
	http.Redirect(w, r, fmt.Sprintf("/grading/%d", id), http.StatusSeeOther)
}

func (h *GradingViewHandler) RecordReturn(w http.ResponseWriter, r *http.Request, id uint) {
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
