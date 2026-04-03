package handlers

import (
	"fmt"
	"html/template"
	"log/slog"
	"net/http"
	"time"
)

var viewFuncs = template.FuncMap{
	"fmtDate": func(t time.Time) string {
		if t.IsZero() {
			return "—"
		}
		return t.Format("Jan 2, 2006")
	},
	"fmtMoney": func(f float64) string {
		return fmt.Sprintf("$%.2f", f)
	},
	"purposeLabel": func(p string) string {
		switch p {
		case "PERSONAL_COLLECTION":
			return "Collection"
		case "IN_GRADING":
			return "In Grading"
		case "GRADED_INVENTORY":
			return "Graded"
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
}

func parseTemplate(page string) *template.Template {
	return template.Must(
		template.New("").Funcs(viewFuncs).ParseFiles(
			"ui/templates/layout.html",
			"ui/templates/"+page+".html",
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
