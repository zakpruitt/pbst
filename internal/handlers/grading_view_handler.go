package handlers

import (
	"fmt"
	"html/template"
	"net/http"
	"strconv"

	"github.com/zakpruitt/pbst/internal/repository"
	"github.com/zakpruitt/pbst/internal/services"
)

type GradingViewHandler struct {
	grading       *template.Template
	gradingNew    *template.Template
	gradingDetail *template.Template
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
		grading:       parseTemplate("grading"),
		gradingNew:    parseTemplate("grading-new"),
		gradingDetail: parseTemplate("grading-detail"),
		gradingRepo:   gradingRepo,
		itemRepo:      itemRepo,
		gradingSvc:    gradingSvc,
	}
}

func (h *GradingViewHandler) Grading(w http.ResponseWriter, r *http.Request) {
	submissions, err := h.gradingRepo.GetAllSubmissions(r.Context())
	if err != nil {
		serverError(w, err)
		return
	}
	execTemplate(w, h.grading, "layout", map[string]any{"Page": "grading", "Submissions": submissions})
}

func (h *GradingViewHandler) GradingNew(w http.ResponseWriter, r *http.Request) {
	unattached, err := h.itemRepo.GetPendingGradeUnattached(r.Context())
	if err != nil {
		serverError(w, err)
		return
	}
	execTemplate(w, h.gradingNew, "layout", map[string]any{
		"Page":       "grading",
		"Unattached": unattached,
	})
}

func (h *GradingViewHandler) CreateGrading(w http.ResponseWriter, r *http.Request) {
	if err := r.ParseForm(); err != nil {
		serverError(w, err)
		return
	}
	itemIDs := parseFormIDs(r.Form, "item_ids")
	submission, err := h.gradingSvc.CreateWithItems(r.Context(), r.FormValue("company"), r.FormValue("submission_method"), itemIDs)
	if err != nil {
		serverError(w, err)
		return
	}
	http.Redirect(w, r, fmt.Sprintf("/grading/%d", submission.ID), http.StatusSeeOther)
}

func (h *GradingViewHandler) GradingDetail(w http.ResponseWriter, r *http.Request) {
	id, err := parsePathID(r, "id")
	if err != nil {
		http.NotFound(w, r)
		return
	}
	submission, err := h.gradingRepo.GetSubmissionByID(r.Context(), id)
	if err != nil {
		serverError(w, err)
		return
	}

	data := map[string]any{
		"Page":       "grading",
		"Submission": submission,
	}
	if submission.Status == "PREPPING" {
		unattached, err := h.itemRepo.GetPendingGradeUnattached(r.Context())
		if err != nil {
			serverError(w, err)
			return
		}
		data["Unattached"] = unattached
	}
	execTemplate(w, h.gradingDetail, "layout", data)
}

func (h *GradingViewHandler) AdvanceGradingStatus(w http.ResponseWriter, r *http.Request) {
	id, err := parsePathID(r, "id")
	if err != nil {
		http.NotFound(w, r)
		return
	}
	if err := h.gradingSvc.AdvanceStatus(r.Context(), id, r.FormValue("new_status")); err != nil {
		serverError(w, err)
		return
	}
	http.Redirect(w, r, fmt.Sprintf("/grading/%d", id), http.StatusSeeOther)
}

func (h *GradingViewHandler) AttachItems(w http.ResponseWriter, r *http.Request) {
	id, err := parsePathID(r, "id")
	if err != nil {
		http.NotFound(w, r)
		return
	}
	if err = r.ParseForm(); err != nil {
		serverError(w, err)
		return
	}
	itemIDs := parseFormIDs(r.Form, "item_ids")
	if len(itemIDs) > 0 {
		if err := h.gradingSvc.AttachItems(r.Context(), id, itemIDs); err != nil {
			serverError(w, err)
			return
		}
	}
	http.Redirect(w, r, fmt.Sprintf("/grading/%d", id), http.StatusSeeOther)
}

func (h *GradingViewHandler) RecordReturn(w http.ResponseWriter, r *http.Request) {
	id, err := parsePathID(r, "id")
	if err != nil {
		http.NotFound(w, r)
		return
	}
	if err = r.ParseForm(); err != nil {
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
