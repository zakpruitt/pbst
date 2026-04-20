package services

import (
	"context"
	"fmt"
	"time"

	"github.com/zakpruitt/pbst/internal/models"
	"github.com/zakpruitt/pbst/internal/repository"
)

type DashboardService interface {
	GetDashboardData(ctx context.Context) (DashboardData, error)
}

type dashboardService struct {
	lotRepo     repository.LotRepository
	saleRepo    repository.SaleRepository
	itemRepo    repository.TrackedItemRepository
	gradingRepo repository.GradingRepository
}

func NewDashboardService(
	lotRepo repository.LotRepository,
	saleRepo repository.SaleRepository,
	itemRepo repository.TrackedItemRepository,
	gradingRepo repository.GradingRepository,
) DashboardService {
	return &dashboardService{
		lotRepo:     lotRepo,
		saleRepo:    saleRepo,
		itemRepo:    itemRepo,
		gradingRepo: gradingRepo,
	}
}

type DashboardData struct {
	Page            string
	TotalSpent      float64
	TotalGross      float64
	TotalNet        float64
	TotalFees       float64
	Margin          float64
	SalesCount      int64
	AvgSale         float64
	GradingCount    int64
	InventoryCount  int64
	InventoryCost   float64
	InventoryMarket float64
	Totals7         repository.RangeTotals
	Totals30        repository.RangeTotals
	MonthLabels     []string
	MonthlySpend    []float64
	MonthlyGross    []float64
	MonthlyNet      []float64
	OriginCounts    []repository.OriginCount
	ItemTypeCounts  []repository.ItemTypeCount
	GradingStatuses []repository.GradingStatusCount
	LotStatuses     []repository.LotStatusCount
	TopSales        []models.Sale
	RecentSales     []models.Sale
	RecentLots      []models.LotPurchase
	VinceTotals     repository.RangeTotals
}

func (s *dashboardService) GetDashboardData(ctx context.Context) (DashboardData, error) {
	data := DashboardData{Page: "dashboard"}

	totalSpent, err := s.lotRepo.GetTotalCostNonRejected(ctx)
	if err != nil {
		return data, fmt.Errorf("total spent: %w", err)
	}
	totalGross, err := s.saleRepo.GetTotalGrossAmount(ctx)
	if err != nil {
		return data, fmt.Errorf("total gross: %w", err)
	}
	totalNet, err := s.saleRepo.GetTotalNetAmount(ctx)
	if err != nil {
		return data, fmt.Errorf("total net: %w", err)
	}
	totalFees, err := s.saleRepo.GetTotalFees(ctx)
	if err != nil {
		return data, fmt.Errorf("total fees: %w", err)
	}

	gradingCount, err := s.itemRepo.CountByPurpose(ctx, "IN_GRADING")
	if err != nil {
		return data, fmt.Errorf("grading count: %w", err)
	}
	inventoryCount, err := s.itemRepo.CountByPurpose(ctx, "INVENTORY")
	if err != nil {
		return data, fmt.Errorf("inventory count: %w", err)
	}
	invCost, invMarket, err := s.itemRepo.InventoryTotals(ctx)
	if err != nil {
		return data, fmt.Errorf("inventory totals: %w", err)
	}

	salesCount, err := s.saleRepo.CountByStatus(ctx, "CONFIRMED")
	if err != nil {
		return data, fmt.Errorf("sales count: %w", err)
	}
	avgSale := 0.0
	if salesCount > 0 {
		avgSale = totalNet / float64(salesCount)
	}

	monthlyRev, err := s.saleRepo.MonthlyRevenue(ctx, 12)
	if err != nil {
		return data, fmt.Errorf("monthly revenue: %w", err)
	}
	monthlySpend, err := s.lotRepo.MonthlySpend(ctx, 12)
	if err != nil {
		return data, fmt.Errorf("monthly spend: %w", err)
	}

	originCounts, err := s.saleRepo.CountByOrigin(ctx)
	if err != nil {
		return data, fmt.Errorf("origin counts: %w", err)
	}
	itemTypeCounts, err := s.itemRepo.CountByItemType(ctx)
	if err != nil {
		return data, fmt.Errorf("item type counts: %w", err)
	}
	gradingStatuses, err := s.gradingRepo.CountByStatus(ctx)
	if err != nil {
		return data, fmt.Errorf("grading status counts: %w", err)
	}
	lotStatuses, err := s.lotRepo.CountByStatus(ctx)
	if err != nil {
		return data, fmt.Errorf("lot status counts: %w", err)
	}

	topSales, err := s.saleRepo.GetTopByNet(ctx, 5)
	if err != nil {
		return data, fmt.Errorf("top sales: %w", err)
	}
	recentSales, err := s.saleRepo.GetRecent(ctx, 5)
	if err != nil {
		return data, fmt.Errorf("recent sales: %w", err)
	}
	recentLots, err := s.lotRepo.GetRecent(ctx, 5)
	if err != nil {
		return data, fmt.Errorf("recent lots: %w", err)
	}

	totals30, err := s.saleRepo.TotalsSince(ctx, time.Now().AddDate(0, 0, -30))
	if err != nil {
		return data, fmt.Errorf("30d totals: %w", err)
	}
	totals7, err := s.saleRepo.TotalsSince(ctx, time.Now().AddDate(0, 0, -7))
	if err != nil {
		return data, fmt.Errorf("7d totals: %w", err)
	}
	vinceTotals, err := s.saleRepo.VinceTotals(ctx)
	if err != nil {
		return data, fmt.Errorf("vince totals: %w", err)
	}

	labels, spendSeries, grossSeries, netSeries := buildMonthlySeries(monthlyRev, monthlySpend, 12)

	data.TotalSpent = totalSpent
	data.TotalGross = totalGross
	data.TotalNet = totalNet
	data.TotalFees = totalFees
	data.Margin = totalNet - totalSpent
	data.SalesCount = salesCount
	data.AvgSale = avgSale
	data.GradingCount = gradingCount
	data.InventoryCount = inventoryCount
	data.InventoryCost = invCost
	data.InventoryMarket = invMarket
	data.Totals7 = totals7
	data.Totals30 = totals30
	data.MonthLabels = labels
	data.MonthlySpend = spendSeries
	data.MonthlyGross = grossSeries
	data.MonthlyNet = netSeries
	data.OriginCounts = originCounts
	data.ItemTypeCounts = itemTypeCounts
	data.GradingStatuses = gradingStatuses
	data.LotStatuses = lotStatuses
	data.TopSales = topSales
	data.RecentSales = recentSales
	data.RecentLots = recentLots
	data.VinceTotals = vinceTotals
	return data, nil
}

// buildMonthlySeries zero-fills a 12-month timeline so the P&L chart has
// aligned x-axis labels even for months with only purchases or only sales.
func buildMonthlySeries(rev []repository.MonthlyRevenue, spend []repository.MonthlySpend, months int) (labels []string, spendSeries, grossSeries, netSeries []float64) {
	labels = make([]string, 0, months)
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
	for _, sp := range spend {
		spendBy[sp.Month] = sp
	}

	spendSeries = make([]float64, len(labels))
	grossSeries = make([]float64, len(labels))
	netSeries = make([]float64, len(labels))
	for i, m := range labels {
		spendSeries[i] = spendBy[m].Spend
		grossSeries[i] = revBy[m].Gross
		netSeries[i] = revBy[m].Net
	}
	return labels, spendSeries, grossSeries, netSeries
}
