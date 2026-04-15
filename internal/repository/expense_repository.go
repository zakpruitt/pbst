package repository

import (
	"context"
	"fmt"

	"github.com/zakpruitt/pbst/internal/models"
	"gorm.io/gorm"
)

type ExpenseRepository struct {
	db *gorm.DB
}

func NewExpenseRepository(db *gorm.DB) *ExpenseRepository {
	return &ExpenseRepository{db: db}
}

func (r *ExpenseRepository) Create(ctx context.Context, e *models.Expense) error {
	if err := r.db.WithContext(ctx).Create(e).Error; err != nil {
		return fmt.Errorf("create expense: %w", err)
	}
	return nil
}

// GetAll returns all expenses newest first so the template can group by month
// contiguously without re-sorting.
func (r *ExpenseRepository) GetAll(ctx context.Context) ([]models.Expense, error) {
	var expenses []models.Expense
	err := r.db.WithContext(ctx).Order("expense_date DESC, id DESC").Find(&expenses).Error
	if err != nil {
		return nil, fmt.Errorf("get all expenses: %w", err)
	}
	return expenses, nil
}

func (r *ExpenseRepository) Delete(ctx context.Context, id uint) error {
	if err := r.db.WithContext(ctx).Delete(&models.Expense{}, id).Error; err != nil {
		return fmt.Errorf("delete expense: %w", err)
	}
	return nil
}
