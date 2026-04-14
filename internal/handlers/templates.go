package handlers

import (
	"fmt"
	"html/template"
	"log/slog"
	"net/http"
	"strings"
	"time"
)

var viewFuncs = template.FuncMap{
	"fmtDate": func(t time.Time) string {
		if t.IsZero() {
			return "—"
		}
		return t.Format("Jan 2, 2006")
	},
	"fmtDateInput": func(t time.Time) string {
		if t.IsZero() {
			return ""
		}
		return t.Format("2006-01-02")
	},
	"fmtMoney": func(f float64) string {
		return fmt.Sprintf("$%.2f", f)
	},
	"purposeLabel": func(p string) string {
		switch p {
		case "INVENTORY":
			return "Inventory"
		case "IN_GRADING":
			return "In Grading"
		case "PENDING_GRADE":
			return "To Grade"
		default:
			return p
		}
	},
	"statusClass": func(s string) string {
		switch s {
		case "ACCEPTED", "RETURNED":
			return "bg-success"
		case "PENDING", "IN_TRANSIT", "PREPPING":
			return "bg-warning text-dark"
		case "REJECTED":
			return "bg-danger"
		default:
			return "bg-secondary"
		}
	},
	"fmtPercent": func(f float64) string {
		return fmt.Sprintf("%.3f%%", f*100)
	},
	"add": func(a, b float64) float64 { return a + b },
	"mul": func(a, b float64) float64 { return a * b },
	"gradeClass": func(company string) string {
		switch company {
		case "PSA":
			return "grade-psa"
		case "BGS":
			return "grade-bgs"
		case "CGC":
			return "grade-cgc"
		case "TAG":
			return "grade-tag"
		default:
			return "bg-secondary"
		}
	},
	"itemTypeLabel": func(t string) string {
		switch t {
		case "RAW_CARD":
			return "Raw"
		case "GRADED_CARD":
			return "Graded"
		case "SEALED_PRODUCT":
			return "Sealed"
		default:
			return t
		}
	},
	"originLabel": func(o string) string {
		switch o {
		case "EBAY":
			return "eBay"
		case "FACEBOOK":
			return "Facebook"
		case "OTHER":
			return "Other"
		default:
			return o
		}
	},
	"originClass": func(o string) string {
		switch o {
		case "EBAY":
			return "bg-primary"
		case "FACEBOOK":
			return "bg-info text-dark"
		default:
			return "bg-secondary"
		}
	},
	"ebayOrderURL": func(orderID, origin string) string {
		if origin != "EBAY" || orderID == "" {
			return ""
		}
		return fmt.Sprintf("https://www.ebay.com/sh/ord/details?orderid=%s", orderID)
	},
	"monthClass": func(t time.Time) string {
		return "month-" + strings.ToLower(t.Month().String())
	},
	"saleStatusClass": func(s string) string {
		switch s {
		case "CONFIRMED":
			return "bg-success"
		case "STAGED":
			return "bg-warning text-dark"
		case "IGNORED":
			return "bg-secondary"
		default:
			return "bg-secondary"
		}
	},
	"saleStatusLabel": func(s string) string {
		switch s {
		case "CONFIRMED":
			return "Confirmed"
		case "STAGED":
			return "Staged"
		case "IGNORED":
			return "Ignored"
		default:
			return s
		}
	},
}

func parseTemplate(page string) *template.Template {
	return template.Must(
		template.New("").Funcs(viewFuncs).ParseFiles(
			"ui/templates/layout.html",
			"ui/templates/"+page+".html",
		),
	)
}

// parseTemplateWithPartial parses a page template together with a partial,
// so the page can call {{template "partial-name" .}} server-side.
func parseTemplateWithPartial(page, partial string) *template.Template {
	return template.Must(
		template.New("").Funcs(viewFuncs).ParseFiles(
			"ui/templates/layout.html",
			"ui/templates/"+page+".html",
			"ui/templates/"+partial+".html",
		),
	)
}

// parsePartialTemplate parses a standalone partial (no layout wrapper).
// The returned template should be executed by its defined name.
func parsePartialTemplate(partial string) *template.Template {
	return template.Must(
		template.New("").Funcs(viewFuncs).ParseFiles(
			"ui/templates/" + partial + ".html",
		),
	)
}

func execTemplate(w http.ResponseWriter, tmpl *template.Template, name string, data any) {
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	if err := tmpl.ExecuteTemplate(w, name, data); err != nil {
		slog.Error("template exec failed", "template", name, "error", err)
	}
}

func serverError(w http.ResponseWriter, err error) {
	slog.Error("view handler error", "error", err)
	http.Error(w, "Internal Server Error", http.StatusInternalServerError)
}

// requirePathID parses the {id} path parameter and writes a 404 if it's missing or invalid.
// Returns (id, true) on success, (0, false) on failure — the caller should return immediately on false.
func requirePathID(w http.ResponseWriter, r *http.Request) (uint, bool) {
	id, err := parsePathID(r, "id")
	if err != nil {
		http.NotFound(w, r)
		return 0, false
	}
	return id, true
}
