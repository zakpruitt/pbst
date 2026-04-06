package handlers

import (
	"html/template"
	"net/http"
	"strconv"
	"time"

	"github.com/zakpruitt/pbst/internal/models"
	"github.com/zakpruitt/pbst/internal/repository"
)

type SaleViewHandler struct {
	tmpl     *template.Template
	saleRepo *repository.SaleRepository
}

func NewSaleViewHandler(saleRepo *repository.SaleRepository) *SaleViewHandler {
	return &SaleViewHandler{
		tmpl:     parseTemplate("sales/index"),
		saleRepo: saleRepo,
	}
}

func (h *SaleViewHandler) Sales(w http.ResponseWriter, r *http.Request) {
	sales, err := h.saleRepo.GetAllSales(r.Context())
	if err != nil {
		serverError(w, err)
		return
	}
	execTemplate(w, h.tmpl, "layout", map[string]any{"Page": "sales", "Sales": sales})
}

func (h *SaleViewHandler) CreateSale(w http.ResponseWriter, r *http.Request) {
	gross, _ := strconv.ParseFloat(r.FormValue("gross_amount"), 64)
	fees, _ := strconv.ParseFloat(r.FormValue("ebay_fees"), 64)
	shipping, _ := strconv.ParseFloat(r.FormValue("shipping_cost"), 64)
	saleDate, _ := time.Parse("2006-01-02", r.FormValue("sale_date"))

	sale := &models.Sale{
		EbayOrderID:   r.FormValue("ebay_order_id"),
		Title:         r.FormValue("title"),
		BuyerUsername: r.FormValue("buyer_username"),
		SaleDate:      saleDate,
		GrossAmount:   gross,
		EbayFees:      fees,
		ShippingCost:  shipping,
		NetAmount:     gross - fees - shipping,
		OrderStatus:   "COMPLETED",
	}
	if err := h.saleRepo.CreateSale(r.Context(), sale); err != nil {
		serverError(w, err)
		return
	}
	sales, err := h.saleRepo.GetAllSales(r.Context())
	if err != nil {
		serverError(w, err)
		return
	}
	execTemplate(w, h.tmpl, "sales-list", map[string]any{"Sales": sales})
}
