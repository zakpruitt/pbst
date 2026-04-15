package handlers

import (
	"encoding/json"
	"log/slog"
	"net/http"

	"github.com/zakpruitt/pbst/internal/repository"
)

func HandleSearchSealed(repo *repository.SealedProductRepository) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		q := r.URL.Query().Get("q")
		if q == "" {
			w.Header().Set("Content-Type", "application/json")
			w.Write([]byte("[]"))
			return
		}

		products, err := repo.Search(r.Context(), q)
		if err != nil {
			slog.Error("sealed search failed", "error", err)
			http.Error(w, "Internal Server Error", http.StatusInternalServerError)
			return
		}

		w.Header().Set("Content-Type", "application/json")
		if err := json.NewEncoder(w).Encode(products); err != nil {
			slog.Error("sealed search encode failed", "error", err)
		}
	}
}
