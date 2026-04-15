package handlers

import (
	"html/template"
	"net/http"
	"time"

	"github.com/zakpruitt/pbst/internal/models"
	"github.com/zakpruitt/pbst/internal/repository"
)

type ExpenseViewHandler struct {
	index       *template.Template
	expenseRepo *repository.ExpenseRepository
}

type ExpenseMonthGroup struct {
	Label    string
	FirstDay time.Time
	Total    float64
	Expenses []models.Expense
}

func NewExpenseViewHandler(expenseRepo *repository.ExpenseRepository) *ExpenseViewHandler {
	return &ExpenseViewHandler{
		index:       parseTemplate("expenses/index"),
		expenseRepo: expenseRepo,
	}
}

func (h *ExpenseViewHandler) Expenses(w http.ResponseWriter, r *http.Request) {
	expenses, err := h.expenseRepo.GetAll(r.Context())
	if err != nil {
		serverError(w, err)
		return
	}
	var total float64
	for _, e := range expenses {
		total += e.Cost
	}
	execTemplate(w, h.index, "layout", map[string]any{
		"Page":   "expenses",
		"Groups": groupExpensesByMonth(expenses),
		"Total":  total,
		"Count":  len(expenses),
	})
}

func (h *ExpenseViewHandler) CreateExpense(w http.ResponseWriter, r *http.Request) {
	if err := r.ParseForm(); err != nil {
		serverError(w, err)
		return
	}
	e := &models.Expense{
		Name:        r.FormValue("name"),
		ExpenseDate: parseFormDate(r, "expense_date"),
		Cost:        parseFormFloat(r, "cost"),
	}
	if e.ExpenseDate.IsZero() {
		e.ExpenseDate = time.Now()
	}
	if err := h.expenseRepo.Create(r.Context(), e); err != nil {
		serverError(w, err)
		return
	}
	http.Redirect(w, r, "/expenses", http.StatusSeeOther)
}

func (h *ExpenseViewHandler) DeleteExpense(w http.ResponseWriter, r *http.Request) {
	id, ok := requirePathID(w, r)
	if !ok {
		return
	}
	if err := h.expenseRepo.Delete(r.Context(), id); err != nil {
		serverError(w, err)
		return
	}
	http.Redirect(w, r, "/expenses", http.StatusSeeOther)
}

// groupExpensesByMonth buckets expenses (assumed sorted by expense_date DESC)
// into contiguous month groups, summing per-month cost for a footer total.
func groupExpensesByMonth(expenses []models.Expense) []ExpenseMonthGroup {
	var groups []ExpenseMonthGroup
	var current string
	for _, e := range expenses {
		label := e.ExpenseDate.Format("January 2006")
		if label != current {
			groups = append(groups, ExpenseMonthGroup{Label: label, FirstDay: e.ExpenseDate})
			current = label
		}
		i := len(groups) - 1
		groups[i].Expenses = append(groups[i].Expenses, e)
		groups[i].Total += e.Cost
	}
	return groups
}
