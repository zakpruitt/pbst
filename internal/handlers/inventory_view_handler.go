package handlers

import (
	"html/template"
	"net/http"

	"github.com/zakpruitt/pbst/internal/repository"
)

type InventoryViewHandler struct {
	tmpl     *template.Template
	itemRepo *repository.TrackedItemRepository
}

func NewInventoryViewHandler(itemRepo *repository.TrackedItemRepository) *InventoryViewHandler {
	return &InventoryViewHandler{
		tmpl:     parseTemplate("inventory/index"),
		itemRepo: itemRepo,
	}
}

func (h *InventoryViewHandler) Inventory(w http.ResponseWriter, r *http.Request) {
	purpose := r.URL.Query().Get("purpose")
	if purpose == "" {
		purpose = "PERSONAL_COLLECTION"
	}
	items, err := h.itemRepo.GetItemsByPurpose(r.Context(), purpose)
	if err != nil {
		serverError(w, err)
		return
	}
	data := map[string]any{"Page": "inventory", "Items": items, "Purpose": purpose}
	if r.Header.Get("HX-Request") != "" {
		execTemplate(w, h.tmpl, "inventory-list", data)
		return
	}
	execTemplate(w, h.tmpl, "layout", data)
}
