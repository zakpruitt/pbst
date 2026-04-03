package main

import (
	"context"
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
	loadEnv()

	database := db.NewConnection()
	store := auth.NewStore()

	lotRepo := repository.NewLotRepository(database)
	saleRepo := repository.NewSaleRepository(database)
	itemRepo := repository.NewTrackedItemRepository(database)
	gradingRepo := repository.NewGradingRepository(database)
	cardRepo := repository.NewPokemonCardRepository(database)

	gradingSvc := services.NewGradingService(gradingRepo, itemRepo)
	lotSvc := services.NewLotService(lotRepo, itemRepo)

	pokeWalletSync := jobs.NewPokeWalletSync(cardRepo, os.Getenv("POKEWALLET_API_KEY"), os.Getenv("POKEWALLET_BASE_URL"))
	go pokeWalletSync.Run(context.Background())

	authHandler := auth.NewHandler(store)
	cardHandler := handlers.NewPokemonCardHandler(cardRepo)

	dash := handlers.NewDashboardViewHandler(lotRepo, saleRepo, itemRepo)
	lots := handlers.NewLotViewHandler(lotRepo, lotSvc)
	sales := handlers.NewSaleViewHandler(saleRepo)
	inv := handlers.NewInventoryViewHandler(itemRepo)
	grading := handlers.NewGradingViewHandler(gradingRepo, itemRepo, gradingSvc)

	mux := http.NewServeMux()
	registerRoutes(mux, store, authHandler, cardHandler, dash, lots, sales, inv, grading)

	slog.Info("server starting", "addr", "http://localhost:8080")
	if err := http.ListenAndServe(":8080", mux); err != nil {
		log.Fatalf("server error: %v", err)
	}
}

// loadEnv loads .env.<APP_ENV> (defaults to .env.development if APP_ENV is unset).
// In production, prefer real environment variables over a file.
func loadEnv() {
	env := os.Getenv("APP_ENV")
	if env == "" {
		env = "development"
	}
	if err := godotenv.Load(".env." + env); err != nil {
		slog.Debug("no env file loaded", "file", ".env."+env)
	}
}

func registerRoutes(
	mux *http.ServeMux,
	store *auth.Store,
	authH *auth.Handler,
	cards *handlers.PokemonCardHandler,
	dash *handlers.DashboardViewHandler,
	lots *handlers.LotViewHandler,
	sales *handlers.SaleViewHandler,
	inv *handlers.InventoryViewHandler,
	grading *handlers.GradingViewHandler,
) {
	apiMW := func(h http.HandlerFunc) http.Handler {
		return auth.RequestLogger(auth.SessionAuth(store)(h))
	}
	viewMW := func(h http.HandlerFunc) http.Handler {
		return auth.RequestLogger(auth.SessionAuthView(store)(h))
	}

	mux.Handle("POST /api/v1/login", auth.RequestLogger(http.HandlerFunc(authH.HandleLogin)))
	mux.Handle("POST /api/v1/logout", auth.RequestLogger(http.HandlerFunc(authH.HandleLogout)))

	mux.Handle("GET /api/v1/cards/search", apiMW(cards.HandleSearchCards))

	mux.Handle("GET /", viewMW(dash.Dashboard))
	mux.Handle("GET /lots", viewMW(lots.Lots))
	mux.Handle("GET /lots/new", viewMW(lots.LotNew))
	mux.Handle("POST /lots", viewMW(lots.SaveLot))
	mux.Handle("GET /lots/{id}", viewMW(lots.LotDetail))
	mux.Handle("GET /lots/{id}/edit", viewMW(lots.LotEditForm))
	mux.Handle("POST /lots/{id}", viewMW(lots.UpdateLot))
	mux.Handle("POST /lots/{id}/status", viewMW(lots.UpdateLotStatus))
	mux.Handle("GET /sales", viewMW(sales.Sales))
	mux.Handle("POST /sales", viewMW(sales.CreateSale))
	mux.Handle("GET /inventory", viewMW(inv.Inventory))
	mux.Handle("GET /grading", viewMW(grading.Grading))
	mux.Handle("GET /grading/new", viewMW(grading.GradingNew))
	mux.Handle("POST /grading", viewMW(grading.CreateGrading))
	mux.Handle("GET /grading/{id}", viewMW(grading.GradingDetail))
	mux.Handle("POST /grading/{id}/advance", viewMW(grading.AdvanceGradingStatus))
	mux.Handle("POST /grading/{id}/items", viewMW(grading.AttachItems))
	mux.Handle("POST /grading/{id}/return", viewMW(grading.RecordReturn))

	mux.Handle("GET /static/", http.StripPrefix("/static/", http.FileServer(http.Dir("./ui/static"))))
	mux.HandleFunc("GET /login.html", func(w http.ResponseWriter, r *http.Request) {
		http.ServeFile(w, r, "./ui/static/login.html")
	})
}
