package handlers

import (
	"context"
	"encoding/json"
	"log/slog"
	"net/http"

	"github.com/zakpruitt/pbst/internal/repository"
)

func HandleSearchCards(repo repository.PokemonCardRepository) http.HandlerFunc {
	return handleSearch(repo.Search, "card")
}

func HandleSearchSealed(repo repository.SealedProductRepository) http.HandlerFunc {
	return handleSearch(repo.Search, "sealed")
}

func handleSearch[T any](
	search func(context.Context, string) ([]T, error),
	label string,
) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		q := r.URL.Query().Get("q")
		w.Header().Set("Content-Type", "application/json")
		if q == "" {
			w.Write([]byte("[]"))
			return
		}

		results, err := search(r.Context(), q)
		if err != nil {
			slog.Error(label+" search failed", "error", err)
			http.Error(w, "Internal Server Error", http.StatusInternalServerError)
			return
		}

		if err := json.NewEncoder(w).Encode(results); err != nil {
			slog.Error(label+" search encode failed", "error", err)
		}
	}
}
