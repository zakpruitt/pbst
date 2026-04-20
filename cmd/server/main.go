package main

import (
	"context"
	"fmt"
	"log"
	"log/slog"
	"net/http"
	"os"

	"github.com/joho/godotenv"

	"github.com/zakpruitt/pbst/internal/auth"
	"github.com/zakpruitt/pbst/internal/db"
	"github.com/zakpruitt/pbst/internal/handlers"
	"github.com/zakpruitt/pbst/internal/jobs"
	"github.com/zakpruitt/pbst/internal/repository"
	"github.com/zakpruitt/pbst/internal/services"
)

func main() {
	env := os.Getenv("APP_ENV")
	if env == "" {
		env = "development"
	}
	if err := godotenv.Load(".env." + env); err != nil {
		slog.Debug("no env file loaded", "file", ".env."+env)
	}
	slog.Info(fmt.Sprintf("APP LAUNCHING IN %s", env))

	// Infrastructure
	database := db.NewConnection()
	store := auth.NewStore()

	// Repositories
	var (
		lotRepo     = repository.NewLotRepository(database)
		saleRepo    = repository.NewSaleRepository(database)
		itemRepo    = repository.NewTrackedItemRepository(database)
		gradingRepo = repository.NewGradingRepository(database)
		cardRepo    = repository.NewPokemonCardRepository(database)
		sealedRepo  = repository.NewSealedProductRepository(database)
		expenseRepo = repository.NewExpenseRepository(database)
	)

	// Services
	lotSvc := services.NewLotService(lotRepo, itemRepo)
	gradingSvc := services.NewGradingService(gradingRepo, itemRepo)
	saleSvc := services.NewSaleService(saleRepo, itemRepo)

	// Background jobs
	backgroundJobs := []jobs.Job{
		jobs.NewPokeWalletSync(cardRepo, os.Getenv("POKEWALLET_API_KEY"), os.Getenv("POKEWALLET_BASE_URL")),
		jobs.NewEbaySalesSync(saleRepo, os.Getenv("EBAY_CLIENT_ID"), os.Getenv("EBAY_CLIENT_SECRET"), os.Getenv("EBAY_REFRESH_TOKEN")),
	}
	for _, job := range backgroundJobs {
		go job.Run(context.Background())
	}

	mux := http.NewServeMux()

	// Middleware
	apiMiddleware := func(h http.HandlerFunc) http.Handler {
		return auth.RequestLogger(auth.SessionAuth(store)(h))
	}
	viewMiddleware := func(h http.HandlerFunc) http.Handler {
		return auth.RequestLogger(auth.SessionAuthView(store)(h))
	}

	// Handlers
	dashboardSvc := services.NewDashboardService(lotRepo, saleRepo, itemRepo, gradingRepo)
	inventorySvc := services.NewInventoryService(itemRepo)
	lots := handlers.NewLotViewHandler(lotSvc)
	sales := handlers.NewSaleViewHandler(saleSvc, inventorySvc)
	grading := handlers.NewGradingViewHandler(gradingSvc, inventorySvc)

	// Auth
	mux.Handle("POST /api/v1/login", auth.RequestLogger(http.HandlerFunc(auth.NewHandler(store).HandleLogin)))
	mux.Handle("POST /api/v1/logout", auth.RequestLogger(http.HandlerFunc(auth.NewHandler(store).HandleLogout)))

	// API
	mux.Handle("GET /api/v1/cards/search", apiMiddleware(handlers.HandleSearchCards(cardRepo))) // API handlers can keep repo access for simple queries
	mux.Handle("GET /api/v1/sealed/search", apiMiddleware(handlers.HandleSearchSealed(sealedRepo)))

	// Dashboard
	mux.Handle("GET /", viewMiddleware(handlers.NewDashboardHandler(dashboardSvc)))

	// Lots
	mux.Handle("GET /lots", viewMiddleware(lots.Lots))
	mux.Handle("GET /lots/new", viewMiddleware(lots.LotNew))
	mux.Handle("POST /lots", viewMiddleware(lots.SaveLot))
	mux.Handle("GET /lots/{id}", viewMiddleware(handlers.WithPathID(lots.LotDetail)))
	mux.Handle("GET /lots/{id}/edit", viewMiddleware(handlers.WithPathID(lots.LotEditForm)))
	mux.Handle("GET /lots/partials/row", viewMiddleware(lots.RowPartial))
	mux.Handle("POST /lots/{id}", viewMiddleware(handlers.WithPathID(lots.UpdateLot)))
	mux.Handle("POST /lots/{id}/status", viewMiddleware(handlers.WithPathID(lots.UpdateLotStatus)))
	mux.Handle("POST /lots/{id}/delete", viewMiddleware(handlers.WithPathID(lots.DeleteLot)))

	// Sales
	mux.Handle("GET /sales", viewMiddleware(sales.Sales))
	mux.Handle("GET /sales/new", viewMiddleware(sales.SaleNew))
	mux.Handle("GET /sales/staging", viewMiddleware(sales.SalesStaging))
	mux.Handle("POST /sales", viewMiddleware(sales.CreateSale))
	mux.Handle("GET /sales/{id}", viewMiddleware(handlers.WithPathID(sales.SaleDetail)))
	mux.Handle("GET /sales/{id}/confirm", viewMiddleware(handlers.WithPathID(sales.SaleConfirmForm)))
	mux.Handle("POST /sales/{id}/confirm", viewMiddleware(handlers.WithPathID(sales.ConfirmSale)))
	mux.Handle("POST /sales/{id}/ignore", viewMiddleware(handlers.WithPathID(sales.IgnoreSale)))
	mux.Handle("POST /sales/{id}/vince", viewMiddleware(handlers.WithPathID(sales.VinceSale)))
	mux.Handle("POST /sales/{id}/unstage", viewMiddleware(handlers.WithPathID(sales.UnstageSale)))
	mux.Handle("POST /sales/{id}/delete", viewMiddleware(handlers.WithPathID(sales.DeleteSale)))

	// Inventory
	inventory := handlers.NewInventoryViewHandler(inventorySvc)
	mux.Handle("GET /inventory", viewMiddleware(inventory.Inventory))
	mux.Handle("GET /inventory/new", viewMiddleware(inventory.InventoryNew))
	mux.Handle("GET /inventory/partials/row", viewMiddleware(inventory.RowPartial))
	mux.Handle("POST /inventory", viewMiddleware(inventory.CreateInventoryItem))
	mux.Handle("GET /inventory/{id}/edit", viewMiddleware(handlers.WithPathID(inventory.InventoryEditForm)))
	mux.Handle("POST /inventory/{id}", viewMiddleware(handlers.WithPathID(inventory.UpdateInventoryItem)))
	mux.Handle("POST /inventory/{id}/delete", viewMiddleware(handlers.WithPathID(inventory.DeleteInventoryItem)))

	// Expenses
	expenses := handlers.NewExpenseViewHandler(expenseRepo)
	mux.Handle("GET /expenses", viewMiddleware(expenses.Expenses))
	mux.Handle("POST /expenses", viewMiddleware(expenses.CreateExpense))
	mux.Handle("POST /expenses/{id}/delete", viewMiddleware(handlers.WithPathID(expenses.DeleteExpense)))

	// Grading
	mux.Handle("GET /grading", viewMiddleware(grading.Grading))
	mux.Handle("GET /grading/new", viewMiddleware(grading.GradingNew))
	mux.Handle("POST /grading", viewMiddleware(grading.CreateGrading))
	mux.Handle("GET /grading/{id}", viewMiddleware(handlers.WithPathID(grading.GradingDetail)))
	mux.Handle("GET /grading/{id}/edit", viewMiddleware(handlers.WithPathID(grading.GradingEditForm)))
	mux.Handle("POST /grading/{id}", viewMiddleware(handlers.WithPathID(grading.UpdateGrading)))
	mux.Handle("POST /grading/{id}/advance", viewMiddleware(handlers.WithPathID(grading.AdvanceGradingStatus)))
	mux.Handle("POST /grading/{id}/return", viewMiddleware(handlers.WithPathID(grading.RecordReturn)))
	mux.Handle("POST /grading/{id}/delete", viewMiddleware(handlers.WithPathID(grading.DeleteGrading)))

	// Static
	mux.Handle("GET /static/", http.StripPrefix("/static/", http.FileServer(http.Dir("./ui/static"))))
	mux.HandleFunc("GET /login.html", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "./ui/static/login.html")
	})

	slog.Info("server starting", "addr", "http://localhost:8080")
	if err := http.ListenAndServe(":8080", mux); err != nil {
		log.Fatalf("server error: %v", err)
	}
}
