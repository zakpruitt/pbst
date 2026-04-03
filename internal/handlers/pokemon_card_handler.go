package handlers

import (
	"encoding/json"
	"log/slog"
	"net/http"

	"github.com/zakpruitt/pbst/internal/repository"
)

type PokemonCardHandler struct {
	repo *repository.PokemonCardRepository
}

func NewPokemonCardHandler(repo *repository.PokemonCardRepository) *PokemonCardHandler {
	return &PokemonCardHandler{repo: repo}
}

func (h *PokemonCardHandler) HandleSearchCards(w http.ResponseWriter, r *http.Request) {
	q := r.URL.Query().Get("q")
	if q == "" {
		w.Header().Set("Content-Type", "application/json")
		w.Write([]byte("[]"))
		return
	}

	cards, err := h.repo.Search(r.Context(), q)
	if err != nil {
		slog.Error("card search failed", "error", err)
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	if err := json.NewEncoder(w).Encode(cards); err != nil {
		slog.Error("card search encode failed", "error", err)
	}
}
