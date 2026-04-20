package handlers

import (
	"fmt"
	"net/http"

	"github.com/zakpruitt/pbst/internal/services"
)

func NewDashboardHandler(
	dashboardSvc services.DashboardService,
) http.HandlerFunc {
	tmpl := parseTemplate("dashboard")
	return func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/" {
			http.NotFound(w, r)
			return
		}

		data, err := dashboardSvc.GetDashboardData(r.Context())
		if err != nil {
			serverError(w, fmt.Errorf("get dashboard data: %w", err))
			return
		}

		execTemplate(w, tmpl, "layout", data)
	}
}
