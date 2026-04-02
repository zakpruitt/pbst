package jobs

import (
	"context"
	"database/sql"
	"encoding/json"
	"fmt"
	"log/slog"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"

	"github.com/zakpruitt/pbst/internal/models"
	"github.com/zakpruitt/pbst/internal/repository"
)

type syncPayload struct {
	Results    []syncCard `json:"results"`
	Pagination struct {
		TotalPages int `json:"total_pages"`
	} `json:"pagination"`
}

type syncCard struct {
	ID       string `json:"id"`
	CardInfo struct {
		Name       string `json:"name"`
		SetName    string `json:"set_name"`
		SetCode    string `json:"set_code"`
		CardNumber string `json:"card_number"`
		Rarity     string `json:"rarity"`
	} `json:"card_info"`
	TcgPlayer struct {
		URL    string `json:"url"`
		Prices []struct {
			MarketPrice float64 `json:"market_price"`
			LowPrice    float64 `json:"low_price"`
		} `json:"prices"`
	} `json:"tcgplayer"`
}

var syncSets = []string{
	"24423", // M-P Promotional Cards
	"24399", // Mega Brave
	"24400", // Mega Symphonia
	"24459", // Inferno X
	"24499", // High Class Pack: MEGA Dream ex
	"24600", // Nihil Zero
	"24653", // Ninja Spinner
}

type PokeWalletSync struct {
	cardRepo *repository.PokemonCardRepository
	apiKey   string
	baseURL  string
	client   *http.Client
}

func NewPokeWalletSync(cardRepo *repository.PokemonCardRepository, apiKey, baseURL string) *PokeWalletSync {
	return &PokeWalletSync{
		cardRepo: cardRepo,
		apiKey:   apiKey,
		baseURL:  baseURL,
		client:   &http.Client{Timeout: 30 * time.Second},
	}
}

// Run syncs all sets immediately then every 5 minutes until ctx is cancelled.
// Call as: go job.Run(ctx)
func (j *PokeWalletSync) Run(ctx context.Context) {
	slog.Info("pokewallet sync started", "interval", "5m", "sets", len(syncSets))
	j.syncAll(ctx)

	ticker := time.NewTicker(5 * time.Minute)
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			j.syncAll(ctx)
		case <-ctx.Done():
			slog.Info("pokewallet sync stopped")
			return
		}
	}
}

func (j *PokeWalletSync) syncAll(ctx context.Context) {
	for _, code := range syncSets {
		if err := j.syncSet(ctx, code); err != nil {
			slog.Error("sync failed", "set_code", code, "error", err)
		}
	}
}

func (j *PokeWalletSync) syncSet(ctx context.Context, setCode string) error {
	slog.Info("syncing set", "set_code", setCode)

	var cards []models.PokemonCard
	page, totalPages := 1, 1

	for page <= totalPages {
		payload, err := j.fetchPage(ctx, setCode, page)
		if err != nil {
			return fmt.Errorf("syncSet page %d: %w", page, err)
		}
		if len(payload.Results) == 0 {
			break
		}
		for _, c := range payload.Results {
			cards = append(cards, toCardModel(c))
		}
		if payload.Pagination.TotalPages > 0 {
			totalPages = payload.Pagination.TotalPages
		}

		page++
		// Brief delay between pages to avoid hammering the API.
		if page <= totalPages {
			select {
			case <-time.After(500 * time.Millisecond):
			case <-ctx.Done():
				return ctx.Err()
			}
		}
	}

	if len(cards) == 0 {
		slog.Info("no cards returned", "set_code", setCode)
		return nil
	}
	err := j.cardRepo.Upsert(ctx, cards)
	if err != nil {
		return fmt.Errorf("syncSet upsert: %w", err)
	}
	slog.Info("set synced", "set_code", setCode, "count", len(cards))
	return nil
}

func (j *PokeWalletSync) fetchPage(ctx context.Context, setCode string, page int) (*syncPayload, error) {
	u, err := url.Parse(j.baseURL + "/search")
	if err != nil {
		return nil, fmt.Errorf("parse URL: %w", err)
	}
	q := u.Query()
	q.Set("q", setCode)
	q.Set("page", strconv.Itoa(page))
	q.Set("limit", "100")
	u.RawQuery = q.Encode()

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, u.String(), nil)
	if err != nil {
		return nil, fmt.Errorf("create request: %w", err)
	}
	req.Header.Set("X-API-Key", j.apiKey)
	req.Header.Set("Accept", "application/json")

	resp, err := j.client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("execute request: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("API returned status %d", resp.StatusCode)
	}

	var payload syncPayload
	if err := json.NewDecoder(resp.Body).Decode(&payload); err != nil {
		return nil, fmt.Errorf("decode response: %w", err)
	}
	return &payload, nil
}

func toCardModel(c syncCard) models.PokemonCard {
	card := models.PokemonCard{
		ID:            c.ID,
		Name:          c.CardInfo.Name,
		SetCode:       c.CardInfo.SetCode,
		SetName:       c.CardInfo.SetName,
		CardNumber:    c.CardInfo.CardNumber,
		Rarity:        c.CardInfo.Rarity,
		LastPriceSync: sql.NullTime{Time: time.Now(), Valid: true},
	}
	if len(c.TcgPlayer.Prices) > 0 {
		card.MarketPrice = c.TcgPlayer.Prices[0].MarketPrice
		card.LowPrice = c.TcgPlayer.Prices[0].LowPrice
	}
	card.ImageURL = tcgImageURL(c.TcgPlayer.URL)
	return card
}

// tcgImageURL extracts the product ID from a TCGPlayer URL and returns the CDN image URL.
// Returns empty string if the URL doesn't contain a product ID.
func tcgImageURL(u string) string {
	_, after, found := strings.Cut(u, "/product/")
	if !found {
		return ""
	}
	productID, _, _ := strings.Cut(after, "?")
	return fmt.Sprintf("https://tcgplayer-cdn.tcgplayer.com/product/%s_in_1000x1000.jpg", productID)
}
