package jobs

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"log/slog"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/zakpruitt/pbst/internal/models"
	"github.com/zakpruitt/pbst/internal/repository"
)

const (
	ebayAPIBase     = "https://api.ebay.com"
	ebayFinanceBase = "https://apiz.ebay.com"
	ebayTokenURL    = "https://api.ebay.com/identity/v1/oauth2/token"
	ebaySyncDays    = 730
	ebayWindowDays  = 90 // Fulfillment API caps creationdate filter at ~90 days per query
)

type ebayAmount struct {
	Value string `json:"value"`
}

type ebayOrder struct {
	OrderID                string `json:"orderId"`
	CreationDate           string `json:"creationDate"`
	OrderFulfillmentStatus string `json:"orderFulfillmentStatus"`
	Buyer                  struct {
		Username string `json:"username"`
	} `json:"buyer"`
	PricingSummary struct {
		Total        ebayAmount `json:"total"`
		DeliveryCost ebayAmount `json:"deliveryCost"`
	} `json:"pricingSummary"`
	LineItems []struct {
		Title string `json:"title"`
	} `json:"lineItems"`
}

type ebayOrdersResponse struct {
	Orders []ebayOrder `json:"orders"`
}

type ebayTransaction struct {
	OrderID         string     `json:"orderId"`
	TransactionType string     `json:"transactionType"`
	Amount          ebayAmount `json:"amount"`
	TotalFeeAmount  ebayAmount `json:"totalFeeAmount"`
}

type ebayTransactionsResponse struct {
	Transactions []ebayTransaction `json:"transactions"`
}

type EbaySalesSync struct {
	saleRepo     *repository.SaleRepository
	clientID     string
	clientSecret string
	refreshToken string
	client       *http.Client

	mu             sync.Mutex
	accessToken    string
	accessTokenExp time.Time
}

func NewEbaySalesSync(saleRepo *repository.SaleRepository, clientID, clientSecret, refreshToken string) *EbaySalesSync {
	return &EbaySalesSync{
		saleRepo:     saleRepo,
		clientID:     clientID,
		clientSecret: clientSecret,
		refreshToken: refreshToken,
		client:       &http.Client{Timeout: 30 * time.Second},
	}
}

// Run syncs immediately then every hour until ctx is cancelled.
func (j *EbaySalesSync) Run(ctx context.Context) {
	if j.clientID == "" || j.clientSecret == "" || j.refreshToken == "" {
		slog.Info("ebay sync skipped: credentials not configured")
		return
	}

	slog.Info("ebay sales sync started", "interval", "1h", "lookback_days", ebaySyncDays)
	if err := j.syncAll(ctx); err != nil {
		slog.Error("ebay sync failed", "error", err)
	}

	ticker := time.NewTicker(1 * time.Hour)
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			if err := j.syncAll(ctx); err != nil {
				slog.Error("ebay sync failed", "error", err)
			}
		case <-ctx.Done():
			slog.Info("ebay sales sync stopped")
			return
		}
	}
}

func (j *EbaySalesSync) syncAll(ctx context.Context) error {
	since := time.Now().UTC().AddDate(0, 0, -ebaySyncDays)

	fees, err := j.fetchFees(ctx, since)
	if err != nil {
		slog.Warn("ebay fees fetch failed, proceeding without fee data", "error", err)
		fees = map[string]float64{}
	}

	orders, err := j.fetchOrders(ctx, since)
	if err != nil {
		return fmt.Errorf("fetch orders: %w", err)
	}
	if len(orders) == 0 {
		slog.Info("ebay sync: no orders in range")
		return nil
	}

	sales := make([]models.Sale, 0, len(orders))
	for _, o := range orders {
		sale, err := toSaleModel(o, fees)
		if err != nil {
			slog.Warn("skipping order", "order_id", o.OrderID, "error", err)
			continue
		}
		sales = append(sales, sale)
	}

	if err := j.saleRepo.Upsert(ctx, sales); err != nil {
		return fmt.Errorf("upsert: %w", err)
	}
	slog.Info("ebay sync complete", "orders", len(orders), "upserted", len(sales))
	return nil
}

// fetchOrders pages through all orders since the given time, in rolling 90-day
// windows to stay under the Fulfillment API's creationdate filter limit.
func (j *EbaySalesSync) fetchOrders(ctx context.Context, since time.Time) ([]ebayOrder, error) {
	var all []ebayOrder
	now := time.Now().UTC()
	for start := since; start.Before(now); start = start.AddDate(0, 0, ebayWindowDays) {
		end := start.AddDate(0, 0, ebayWindowDays)
		if end.After(now) {
			end = now
		}
		batch, err := j.fetchOrdersWindow(ctx, start, end)
		if err != nil {
			return nil, err
		}
		all = append(all, batch...)
	}
	return all, nil
}

func (j *EbaySalesSync) fetchOrdersWindow(ctx context.Context, from, to time.Time) ([]ebayOrder, error) {
	var all []ebayOrder
	offset, limit := 0, 50
	filter := fmt.Sprintf("creationdate:[%s..%s]",
		from.Format("2006-01-02T15:04:05.000Z"),
		to.Format("2006-01-02T15:04:05.000Z"))

	for {
		u, _ := url.Parse(ebayAPIBase + "/sell/fulfillment/v1/order")
		q := u.Query()
		q.Set("limit", strconv.Itoa(limit))
		q.Set("offset", strconv.Itoa(offset))
		q.Set("filter", filter)
		u.RawQuery = q.Encode()

		var result ebayOrdersResponse
		empty, err := j.getJSON(ctx, u.String(), &result)
		if err != nil {
			return nil, err
		}
		if empty {
			break
		}

		all = append(all, result.Orders...)
		if len(result.Orders) < limit {
			break
		}
		offset += limit
	}
	return all, nil
}

// fetchFees sums all selling costs per orderId: transaction fees from SALE
// transactions plus shipping labels and ad fees from SHIPPING_LABEL /
// NON_SALE_CHARGE transactions.
func (j *EbaySalesSync) fetchFees(ctx context.Context, since time.Time) (map[string]float64, error) {
	fees := make(map[string]float64)
	offset, limit := 0, 200
	filter := fmt.Sprintf("transactionDate:[%s..]", since.Format("2006-01-02T15:04:05.000Z"))

	for {
		u, _ := url.Parse(ebayFinanceBase + "/sell/finances/v1/transaction")
		q := u.Query()
		q.Set("limit", strconv.Itoa(limit))
		q.Set("offset", strconv.Itoa(offset))
		q.Set("filter", filter)
		u.RawQuery = q.Encode()

		var result ebayTransactionsResponse
		empty, err := j.getJSON(ctx, u.String(), &result)
		if err != nil {
			return nil, err
		}
		if empty {
			break
		}

		for _, t := range result.Transactions {
			if t.OrderID == "" {
				continue
			}
			switch t.TransactionType {
			case "SALE":
				fee, _ := strconv.ParseFloat(t.TotalFeeAmount.Value, 64)
				fees[t.OrderID] += fee
			case "SHIPPING_LABEL", "NON_SALE_CHARGE":
				charge, _ := strconv.ParseFloat(t.Amount.Value, 64)
				if charge < 0 {
					charge = -charge
				}
				fees[t.OrderID] += charge
			}
		}

		if len(result.Transactions) < limit {
			break
		}
		offset += limit
	}
	return fees, nil
}

// getJSON performs an authenticated GET and decodes the JSON body into out.
// Returns empty=true on HTTP 204 (no content) so callers can stop paginating.
func (j *EbaySalesSync) getJSON(ctx context.Context, u string, out any) (bool, error) {
	token, err := j.getAccessToken(ctx)
	if err != nil {
		return false, fmt.Errorf("token: %w", err)
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, u, nil)
	if err != nil {
		return false, fmt.Errorf("build request: %w", err)
	}
	req.Header.Set("Authorization", "Bearer "+token)

	resp, err := j.client.Do(req)
	if err != nil {
		return false, fmt.Errorf("request: %w", err)
	}
	defer resp.Body.Close()

	switch resp.StatusCode {
	case http.StatusNoContent:
		return true, nil
	case http.StatusOK:
		if err := json.NewDecoder(resp.Body).Decode(out); err != nil {
			return false, fmt.Errorf("decode: %w", err)
		}
		return false, nil
	default:
		body, _ := io.ReadAll(resp.Body)
		return false, fmt.Errorf("ebay API returned %d: %s", resp.StatusCode, body)
	}
}

func (j *EbaySalesSync) getAccessToken(ctx context.Context) (string, error) {
	j.mu.Lock()
	defer j.mu.Unlock()

	if j.accessToken != "" && time.Now().Before(j.accessTokenExp) {
		return j.accessToken, nil
	}

	body := url.Values{}
	body.Set("grant_type", "refresh_token")
	body.Set("refresh_token", j.refreshToken)

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, ebayTokenURL, strings.NewReader(body.Encode()))
	if err != nil {
		return "", fmt.Errorf("build token request: %w", err)
	}
	creds := base64.StdEncoding.EncodeToString([]byte(j.clientID + ":" + j.clientSecret))
	req.Header.Set("Authorization", "Basic "+creds)
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")

	resp, err := j.client.Do(req)
	if err != nil {
		return "", fmt.Errorf("token request: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		return "", fmt.Errorf("token endpoint returned %d: %s", resp.StatusCode, body)
	}

	var result struct {
		AccessToken string `json:"access_token"`
		ExpiresIn   int    `json:"expires_in"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return "", fmt.Errorf("decode token response: %w", err)
	}

	j.accessToken = result.AccessToken
	j.accessTokenExp = time.Now().Add(time.Duration(result.ExpiresIn-60) * time.Second)
	return j.accessToken, nil
}

func toSaleModel(o ebayOrder, fees map[string]float64) (models.Sale, error) {
	saleDate, err := time.Parse(time.RFC3339Nano, o.CreationDate)
	if err != nil {
		return models.Sale{}, fmt.Errorf("parse date %q: %w", o.CreationDate, err)
	}

	// pricingSummary.total excludes eBay-collected sales tax (managed payments),
	// so gross is already the seller's pre-fee take. Net is simply gross minus
	// all selling costs aggregated into fees[orderID].
	gross, _ := strconv.ParseFloat(o.PricingSummary.Total.Value, 64)
	shipping, _ := strconv.ParseFloat(o.PricingSummary.DeliveryCost.Value, 64)
	ebayFees := fees[o.OrderID]
	net := gross - ebayFees

	var title string
	if len(o.LineItems) > 0 {
		title = o.LineItems[0].Title
	}

	return models.Sale{
		EbayOrderID:   o.OrderID,
		SaleDate:      saleDate,
		Title:         title,
		BuyerUsername: o.Buyer.Username,
		GrossAmount:   gross,
		EbayFees:      ebayFees,
		ShippingCost:  shipping,
		NetAmount:     net,
		OrderStatus:   o.OrderFulfillmentStatus,
	}, nil
}
