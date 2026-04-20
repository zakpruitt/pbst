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
	expenseRepo repository.ExpenseRepository
}

func NewExpenseViewHandler(expenseRepo repository.ExpenseRepository) *ExpenseViewHandler {
	return &ExpenseViewHandler{
		index:       parseTemplate("expenses/index"),
		expenseRepo: expenseRepo,
	}
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
	groups := groupByMonth(expenses, func(e models.Expense) time.Time { return e.ExpenseDate })
	execTemplate(w, h.index, "layout", map[string]any{
		"Page":   "expenses",
		"Groups": groups,
		"Total":  total,
		"Count":  len(expenses),
	})
}

func (h *ExpenseViewHandler) DeleteExpense(w http.ResponseWriter, r *http.Request, id uint) {
	if err := h.expenseRepo.Delete(r.Context(), id); err != nil {
		serverError(w, err)
		return
	}
	http.Redirect(w, r, "/expenses", http.StatusSeeOther)
}
