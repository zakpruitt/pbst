package handlers

import (
	"log/slog"
	"net/http"
	"time"

	"github.com/zakpruitt/pbst/internal/repository"
)

// monthlySeries merges lot spend and sale revenue into a single zero-filled
// timeline so the P&L chart has aligned x-axis labels even when a month has
// only purchases or only sales.
type monthlySeries struct {
	Labels []string
	Spend  []float64
	Gross  []float64
	Net    []float64
}

func NewDashboardHandler(
	lotRepo *repository.LotRepository,
	saleRepo *repository.SaleRepository,
	itemRepo *repository.TrackedItemRepository,
	gradingRepo *repository.GradingRepository,
) http.HandlerFunc {
	tmpl := parseTemplate("dashboard")
	return func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/" {
			http.NotFound(w, r)
			return
		}
		ctx := r.Context()

		totalSpent, err := lotRepo.GetTotalCostNonRejected(ctx)
		if err != nil {
			slog.Error("dashboard: total spent", "error", err)
		}
		totalGross, err := saleRepo.GetTotalGrossAmount(ctx)
		if err != nil {
			slog.Error("dashboard: total gross", "error", err)
		}
		totalNet, err := saleRepo.GetTotalNetAmount(ctx)
		if err != nil {
			slog.Error("dashboard: total net", "error", err)
		}
		totalFees, err := saleRepo.GetTotalFees(ctx)
		if err != nil {
			slog.Error("dashboard: total fees", "error", err)
		}

		gradingCount, err := itemRepo.CountByPurpose(ctx, "IN_GRADING")
		if err != nil {
			slog.Error("dashboard: grading count", "error", err)
		}
		inventoryCount, err := itemRepo.CountByPurpose(ctx, "INVENTORY")
		if err != nil {
			slog.Error("dashboard: inventory count", "error", err)
		}
		invCost, invMarket, err := itemRepo.InventoryTotals(ctx)
		if err != nil {
			slog.Error("dashboard: inventory totals", "error", err)
		}

		salesCount, err := saleRepo.CountByStatus(ctx, "CONFIRMED")
		if err != nil {
			slog.Error("dashboard: sales count", "error", err)
		}
		avgSale := 0.0
		if salesCount > 0 {
			avgSale = totalNet / float64(salesCount)
		}

		monthlyRev, err := saleRepo.MonthlyRevenue(ctx, 12)
		if err != nil {
			slog.Error("dashboard: monthly revenue", "error", err)
		}
		monthlySpend, err := lotRepo.MonthlySpend(ctx, 12)
		if err != nil {
			slog.Error("dashboard: monthly spend", "error", err)
		}
		series := buildMonthlySeries(monthlyRev, monthlySpend, 12)

		originCounts, err := saleRepo.CountByOrigin(ctx)
		if err != nil {
			slog.Error("dashboard: origin counts", "error", err)
		}
		itemTypeCounts, err := itemRepo.CountByItemType(ctx)
		if err != nil {
			slog.Error("dashboard: item type counts", "error", err)
		}
		gradingStatusCounts, err := gradingRepo.CountByStatus(ctx)
		if err != nil {
			slog.Error("dashboard: grading status counts", "error", err)
		}
		lotStatusCounts, err := lotRepo.CountByStatus(ctx)
		if err != nil {
			slog.Error("dashboard: lot status counts", "error", err)
		}

		topSales, err := saleRepo.GetTopByNet(ctx, 5)
		if err != nil {
			slog.Error("dashboard: top sales", "error", err)
		}
		recentSales, err := saleRepo.GetRecent(ctx, 5)
		if err != nil {
			slog.Error("dashboard: recent sales", "error", err)
		}
		recentLots, err := lotRepo.GetRecent(ctx, 5)
		if err != nil {
			slog.Error("dashboard: recent lots", "error", err)
		}

		since30 := time.Now().AddDate(0, 0, -30)
		totals30, err := saleRepo.TotalsSince(ctx, since30)
		if err != nil {
			slog.Error("dashboard: 30d totals", "error", err)
		}
		since7 := time.Now().AddDate(0, 0, -7)
		totals7, err := saleRepo.TotalsSince(ctx, since7)
		if err != nil {
			slog.Error("dashboard: 7d totals", "error", err)
		}

		vinceTotals, err := saleRepo.VinceTotals(ctx)
		if err != nil {
			slog.Error("dashboard: vince totals", "error", err)
		}

		execTemplate(w, tmpl, "layout", map[string]any{
			"Page":              "dashboard",
			"TotalSpent":        totalSpent,
			"TotalGross":        totalGross,
			"TotalNet":          totalNet,
			"TotalFees":         totalFees,
			"Margin":            totalNet - totalSpent,
			"SalesCount":        salesCount,
			"AvgSale":           avgSale,
			"GradingCount":      gradingCount,
			"InventoryCount":    inventoryCount,
			"InventoryCost":     invCost,
			"InventoryMarket":   invMarket,
			"Totals7":           totals7,
			"Totals30":          totals30,
			"MonthLabels":       series.Labels,
			"MonthlySpend":      series.Spend,
			"MonthlyGross":      series.Gross,
			"MonthlyNet":        series.Net,
			"OriginCounts":      originCounts,
			"ItemTypeCounts":    itemTypeCounts,
			"GradingStatuses":   gradingStatusCounts,
			"LotStatuses":       lotStatusCounts,
			"TopSales":          topSales,
			"RecentSales":       recentSales,
			"RecentLots":        recentLots,
			"VinceTotals":       vinceTotals,
		})
	}
}

func buildMonthlySeries(rev []repository.MonthlyRevenue, spend []repository.MonthlySpend, months int) monthlySeries {
	labels := make([]string, 0, months)
	now := time.Now()
	for i := months - 1; i >= 0; i-- {
		t := time.Date(now.Year(), now.Month(), 1, 0, 0, 0, 0, time.UTC).AddDate(0, -i, 0)
		labels = append(labels, t.Format("2006-01"))
	}
	revBy := make(map[string]repository.MonthlyRevenue, len(rev))
	for _, r := range rev {
		revBy[r.Month] = r
	}
	spendBy := make(map[string]repository.MonthlySpend, len(spend))
	for _, s := range spend {
		spendBy[s.Month] = s
	}
	s := monthlySeries{
		Labels: labels,
		Spend:  make([]float64, len(labels)),
		Gross:  make([]float64, len(labels)),
		Net:    make([]float64, len(labels)),
	}
	for i, m := range labels {
		s.Spend[i] = spendBy[m].Spend
		s.Gross[i] = revBy[m].Gross
		s.Net[i] = revBy[m].Net
	}
	return s
}

