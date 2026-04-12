package handlers

import (
	"net/http"

	"github.com/zakpruitt/pbst/internal/repository"
)

func NewInventoryHandler(itemRepo *repository.TrackedItemRepository) http.HandlerFunc {
	tmpl := parseTemplate("inventory/index")
	return func(w http.ResponseWriter, r *http.Request) {
		purpose := r.URL.Query().Get("purpose")
		if purpose == "" {
			purpose = "INVENTORY"
		}
		items, err := itemRepo.GetItemsByPurpose(r.Context(), purpose)
		if err != nil {
			serverError(w, err)
			return
		}
		data := map[string]any{"Page": "inventory", "Items": items, "Purpose": purpose}
		if r.Header.Get("HX-Request") != "" {
			execTemplate(w, tmpl, "inventory-list", data)
			return
		}
		execTemplate(w, tmpl, "layout", data)
	}
}
